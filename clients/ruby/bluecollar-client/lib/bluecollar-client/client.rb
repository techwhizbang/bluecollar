module Bluecollar
  class Client

    attr_accessor :redis_key_prefix, :redis_hostname, :redis_port, :redis_db, :redis_timeout

    class << self

      attr_reader :configuration, :instance

      def configure(options = {})
        client_options = HashWithIndifferentAccess.new(options)
        @configuration = {}
        @configuration[:redis_key_prefix] = client_options[:redis_key_prefix] || "bluecollar"
        @configuration[:redis_hostname] = client_options[:redis_hostname] || "127.0.0.1"
        @configuration[:redis_port] = (client_options[:redis_port] || 6379).to_i
        @configuration[:redis_db] = client_options[:redis_db].to_i
        @configuration[:redis_timeout] = (client_options[:redis_timeout] || 5000).to_i
      end

      def instance
        @instance ||= new(@configuration)
      end
    end

    def async_job_for(worker_name, args)
      redis_connection.lpush processing_queue, redis_payload(worker_name, args)
    end

    private

    def initialize(options)
      options.each { |k,v| send("#{k}=".to_sym, v) }
    end

    def redis_payload(worker_name, args)
      payload = { worker: worker_name, args: args }
      JSON.dump(payload)
    end

    def processing_queue
       @processing_queue ||= "#{self.redis_key_prefix}:processing-queue:default"
    end

    def redis_connection
      @redis_connection ||= Redis.new(host: self.redis_hostname,
                                      port: self.redis_port,
                                      db: self.redis_db,
                                      timeout: self.redis_timeout)
    end
  end
end