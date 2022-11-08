package org.parboiled.util;

import java.util.List;
import java.util.function.BiFunction;

import org.parboiled.Node;
import org.parboiled.Parboiled;
import org.parboiled.json.BaseJsonParser;
import org.parboiled.parserunners.BasicParseRunner;
import org.parboiled.parserunners.ParseRunner;
import org.parboiled.parserunners.TracingParseRunner;

public class ParseUtils {

	public static ParseRunner<?> createParseRunner(boolean trace, Class<? extends BaseJsonParser> parserClass) {
		BaseJsonParser parser = Parboiled.createParser(parserClass);
		ParseRunner<?> runner;
		if (trace) {
			runner = new TracingParseRunner<>(parser.start());
		} else {
			runner = new BasicParseRunner<>(parser.start());
		}
		return runner;
	}

	public static void visitTree(Node<?> node, BiFunction<Node<?>, Integer, Boolean> visitor) {
		visitTree(node, 1, visitor);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void visitTree(Node<?> node, int level, BiFunction<Node<?>, Integer, Boolean> visitor) {
		boolean proceed = visitor.apply(node, level);
		level++;
		if (proceed) {
			visitTree((List) node.getChildren(), level, visitor);
		}
	}

	public static void visitTree(List<Node<?>> children, int level, BiFunction<Node<?>, Integer, Boolean> visitor) {
		for (Node<?> sub : children) {
			visitTree(sub, level, visitor);
		}
	}

	public static void applyChildren(List<Node<?>> children, int level, BiFunction<Node<?>, Integer, Boolean> fn) {
		for (Node<?> sub : children) {
			fn.apply(sub, level);
		}
	}

}