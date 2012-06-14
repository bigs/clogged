# clogged

A multithreaded, agent driven logger written in clojure

## Usage

lein run path/to/config.clj

or

lein trampoline run path/to/config.clj

config files should be clojure files defining a map called `config` that
has the properties `:port`, `:filename` and `:secret`.

## License

Copyright © 2012 Cole Brown

Distributed under the Eclipse Public License, the same as Clojure.
