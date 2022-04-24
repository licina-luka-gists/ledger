#!make
default:
	clj -M -e "(compile 'ledger.core)"
	clj -M:uberdeps --main-class ledger.core
binary:
	src/bash/ledger/compile.sh
