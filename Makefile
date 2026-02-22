build-RickshawAnalysisFunction:
	mvn clean package
	cp target/function.zip $(ARTIFACTS_DIR)
