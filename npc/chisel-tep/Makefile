MODULE ?=
SRC = ./generated/$(MODULE)/*.v
OBJ = ../vsrc/

run:
	sbt run 
	cp $(SRC) $(OBJ)

test:
	sbt test

.PHONY: clean

clean:
	rm -rf target test_run_dir generated
