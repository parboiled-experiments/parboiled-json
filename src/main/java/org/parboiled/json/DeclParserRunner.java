package org.parboiled.json;

import org.parboiled.Parboiled;
import org.parboiled.parserunners.AbstractParseRunner;
import org.parboiled.parserunners.ReportingParseRunner;
import org.parboiled.parserunners.TracingParseRunner;
import org.parboiled.support.ParsingResult;

public class DeclParserRunner<V> {

	private AbstractParseRunner<?> runner;

	public DeclParserRunner(Class<? extends DeclParser<V>> parserClass) {
		DeclParser<V> parser = Parboiled.createParser(parserClass);
		runner = new ReportingParseRunner<>(parser.start());
//		runner = new TracingParseRunner<>(parser.start());
	}

	public ParsingResult<?> parse(String string) {
		return runner.run(string);
	}

}
