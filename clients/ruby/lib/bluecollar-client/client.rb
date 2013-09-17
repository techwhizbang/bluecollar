module Bluecollar
  class Client

    attr_accessor :redis_key_prefix, :redis_hostname, :redis_port, :redis_db, :redis_timeout

    class << self

      attr_reader :configuration, :logger, :instance

      def configure(options = {})
        @instance = nil

        @logger = options[:logger] || Logger.new("/dev/null")

        @configuration = {}
        @configuration[:redis_key_prefix] = options[:redis_key_prefix] || "bluecollar"
        @configuration[:redis_hostname] = options[:redis_hostname] || "127.0.0.1"
        @configuration[:redis_port] = (options[:redis_port] || 6379).to_i
        @configuration[:redis_db] = options[:redis_db].to_i
        @configuration[:redis_timeout] = (options[:redis_timeout] || 5000).to_i
      end

      def instance
        @instance ||= new(@configuration)
      end
    end

    def async_job_for(worker_name, args)
      redis_connection.lpush processing_queue, redis_payload(worker_name, args)
    rescue Redis::BaseError => e
      Bluecollar::Client.logger.warn("Error while adding #{worker_name}: #{args.inspect} to queue.\n#{e}:#{e.message}\nBacktrace:\n\t#{e.backtrace.join("\n\t")}")
      raise Bluecollar::ClientError.new("Error while adding #{worker_name}: #{args.inspect} to queue.\n#{e}:#{e.message}\nBacktrace:\n\t#{e.backtrace.join("\n\t")}")
    end

    private

    def initialize(options)
      options.each { |k,v| send("#{k}=".to_sym, v) }
    end

    def redis_payload(worker_name, args)
      raise ArgumentError.new("args must be an Array.") unless args.is_a? Array

      begin
        payload = { "worker" => worker_name, "args" => args, "uuid" => SecureRandom.uuid, "scheduled-runtime" => nil }
        JSON.dump(payload)
      rescue
        raise Bluecollar::ClientError.new("Error while creating JSON payload for #{worker_name}: #{args.inpsect}.\n#{e}:#{e.message}\nBacktrace:\n\t#{e.backtrace.join("\n\t")}")
      end
    end

    def processing_queue
      @processing_queue ||= "#{redis_key_prefix}:queues:master"
    end

    def redis_connection
      @redis_connection ||= Redis.new(host: redis_hostname,
                                      port: redis_port,
                                      db: redis_db,
                                      timeout: redis_timeout)
    end
  end
end
