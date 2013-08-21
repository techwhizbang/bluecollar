# Bluecollar::Client

TODO: Write a gem description

## Installation

Add this line to your application's Gemfile:

    gem 'bluecollar-client'

And then execute:

    $ bundle

Or install it yourself as:

    $ gem install bluecollar-client

## Usage

`Bluecollar::Client` is intended to act as a ruby interface to `bluecollar.core`. It basically pushes job messages to Redis that are picked up and processed by `bluecollar.core`.

In order to start using `Bluecollar::Client`:

```ruby
require "bluecollar-client"

# to configure client
Bluecollar::Client.configure(redis_key_prefix: "optional_prefix",
                             redis_hostname: "hostname",
                             redis_port: 6379,
                             redis_db: 6,
                             redis_timeout: 5000)

# create async job
Bluecollar::Client.instance.async_job_for(worker_name, args_hash)
```


## Contributing

1. Fork it
2. Create your feature branch (`git checkout -b my-new-feature`)
3. Commit your changes (`git commit -am 'Add some feature'`)
4. Push to the branch (`git push origin my-new-feature`)
5. Create new Pull Request
