# coding: utf-8
lib = File.expand_path('../lib', __FILE__)
$LOAD_PATH.unshift(lib) unless $LOAD_PATH.include?(lib)
require 'bluecollar-client/version'

Gem::Specification.new do |spec|
  spec.name          = "bluecollar-client"
  spec.version       = Bluecollar::VERSION
  spec.authors       = ["Denny Quesada"]
  spec.email         = ["denny.quesada@bookrenter.com"]
  spec.description   = %q{Bluecollar client written in Ruby}
  spec.summary       = %q{Bluecollar client written in Ruby}
  spec.homepage      = ""
  spec.license       = "EPL"

  spec.files         = `git ls-files`.split($/)
  spec.executables   = spec.files.grep(%r{^bin/}) { |f| File.basename(f) }
  spec.test_files    = spec.files.grep(%r{^(test|spec|features)/})
  spec.require_paths = ["lib"]

  spec.add_runtime_dependency "redis"
  spec.add_runtime_dependency %q<json>
  spec.add_development_dependency "bundler", "~> 1.3"
  spec.add_development_dependency "rake"
  spec.add_development_dependency %q<rspec>, ["~> 2.11.0"]
  spec.add_development_dependency %q<pry>
  spec.add_development_dependency %q<faker>
end
