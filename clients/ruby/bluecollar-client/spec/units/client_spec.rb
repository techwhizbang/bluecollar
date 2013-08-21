require 'spec_helper'

describe Bluecollar::Client do

  before do
    Bluecollar::Client.configure(redis_key_prefix: "test_prefix",
                                 redis_hostname: "test_hostname",
                                 redis_port: 4567,
                                 redis_db: 99,
                                 redis_timeout: 1000)
  end

  describe ".initialize" do
    subject { Bluecollar::Client.instance }

    its(:redis_key_prefix) { should == "test_prefix" }
    its(:redis_hostname) { should == "test_hostname" }
    its(:redis_port) { should == 4567 }
    its(:redis_db) { should == 99 }
    its(:redis_timeout) { should == 1000 }
  end

  describe "#processing_queue" do
    subject { Bluecollar::Client.instance.send(:processing_queue) }

    it { should == "test_prefix:processing-queue:default" }
  end

  describe "#redis_connection" do
    subject { Bluecollar::Client.instance.send(:redis_connection) }

    it { should be_a Redis }
  end

  describe "#redis_payload" do
    subject { JSON.parse Bluecollar::Client.instance.send(:redis_payload, 'lazy_worker', {arg1:"first"}) }

    it "should have the expected worker" do
      subject['worker'].should eq("lazy_worker")
    end
    it "should have the expected args" do
      subject['args']['arg1'].should eq("first")
    end
  end

  describe "#async_job_for" do
    subject {  Bluecollar::Client.instance }

    it "should push the data to the right queue" do
      payload = subject.send(:redis_payload, 'lazy_worker', {arg1:"first"})
      queue = subject.send :processing_queue
      mock = double('redis')
      Bluecollar::Client.any_instance.stub(:redis_connection){ mock }
      mock.should_receive(:lpush).with(queue, payload)
      subject.async_job_for('lazy_worker', {arg1:"first"})
    end

    context "when a BaseError is raised" do
      before do
        mock = double('redis')
        Bluecollar::Client.any_instance.stub(:redis_connection){ mock }
        mock.should_receive(:lpush) { raise Redis::BaseError.new("BaseError") }
      end

      subject { Bluecollar::Client.instance.async_job_for('lazy_worker', {arg1:"first"}) }

      it "should raise an error" do
        expect { subject }.to raise_error(Bluecollar::ClientError)
      end
    end
  end
end
