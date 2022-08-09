#!make

.PHONY: jar bin install

default:
	make jar && make bin
jar:
	clj -M -e "(compile 'ledger.core)"
	clj -M:uberdeps --main-class ledger.core
bin:
	src/bash/ledger/compile.sh
install:
	cp bin/ledger ~/.local/bin/ledger
