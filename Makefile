#!make
default:
	clj -M -e "(compile 'ledger.core)"
	clj -M:uberdeps --main-class ledger.core
