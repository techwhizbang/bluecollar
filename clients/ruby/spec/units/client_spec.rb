require 'spec_helper'

describe Bluecollar::Client do

  before(:each) do
    Bluecollar::Client.configure
  end

  describe ".initialize" do
    context "with configuration data" do
      before do
        Bluecollar::Client.configure
      end

      subject { Bluecollar::Client.instance }

      its(:redis_key_prefix) { should == "bluecollar" }
      its(:redis_hostname) { should == "127.0.0.1" }
      its(:redis_port) { should == 6379 }
      its(:redis_db) { should == 0 }
      its(:redis_timeout) { should == 5000 }
    end

    context "with configuration data" do
      before do
        Bluecollar::Client.configure(redis_key_prefix: "test_prefix",
                                     redis_hostname: "test_hostname",
                                     redis_port: 4567,
                                     redis_db: 99,
                                     redis_timeout: 1000)
      end

      subject { Bluecollar::Client.instance }

      its(:redis_key_prefix) { should == "test_prefix" }
      its(:redis_hostname) { should == "test_hostname" }
      its(:redis_port) { should == 4567 }
      its(:redis_db) { should == 99 }
      its(:redis_timeout) { should == 1000 }
    end
  end

  describe "#processing_queue" do

    subject { Bluecollar::Client.instance.send(:processing_queue) }

    context "when redis_key_prefix is set" do
      before do
        Bluecollar::Client.configure(redis_key_prefix: "test_prefix")
      end

      it { should == "test_prefix:queues:master" }
    end

    context "when no prefix is provided" do
      it { should == "bluecollar:queues:master" }
    end
  end

  describe "#redis_connection" do
    subject { Bluecollar::Client.instance.send(:redis_connection) }

    it { should be_a Redis }
  end

  describe "#redis_payload" do
    context "with valid arguments" do
      before do
        SecureRandom.stub(:uuid).and_return("UUID")
      end

      subject { JSON.parse Bluecollar::Client.instance.send(:redis_payload, 'lazy_worker', [{arg1:"first"}]) }

      it "should have the expected worker" do
        subject['worker'].should eq("lazy_worker")
      end

      it "should have the expected args" do
        subject['args'].first['arg1'].should eq("first")
      end

      it "should have the expected UUID" do
        subject['uuid'].should eq("UUID")
      end

      it "should have the expected scheduled-runtime" do
        subject['scheduled-runtime'].should eq(nil)
      end
    end

    context "with a args argument that is not an Array" do
      subject { JSON.parse Bluecollar::Client.instance.send(:redis_payload, 'lazy_worker', {arg1:"first"}) }

      it "should raise an error" do
        expect { subject }.to raise_error(ArgumentError)
      end
    end
  end

  describe "#async_job_for" do
    before do
      SecureRandom.stub(:uuid).and_return("UUID")
    end

    subject {  Bluecollar::Client.instance }

    it "should push the data to the right queue" do
      payload = subject.send(:redis_payload, 'lazy_worker', [{arg1:"first"}])
      queue = subject.send :processing_queue
      mock = double('redis')
      Bluecollar::Client.any_instance.stub(:redis_connection){ mock }
      mock.should_receive(:lpush).with(queue, payload)
      subject.async_job_for('lazy_worker', [{arg1:"first"}])
    end

    context "when a BaseError is raised" do
      before do
        mock = double('redis')
        Bluecollar::Client.any_instance.stub(:redis_connection){ mock }
        mock.should_receive(:lpush) { raise Redis::BaseError.new("BaseError") }
      end

      subject { Bluecollar::Client.instance.async_job_for('lazy_worker', [{arg1:"first"}]) }

      it "should raise an error" do
        expect { subject }.to raise_error(Bluecollar::ClientError)
      end
    end
  end
end
