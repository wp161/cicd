package neu.cs6510.configservice.utils;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import lombok.Setter;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.error.YAMLException;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;

/**
 * Custom YAML constructor that extends SnakeYAML's {@link SafeConstructor} to prevent
 * duplicate keys in YAML files. If a duplicate key is detected, a {@link YAMLException}
 * is thrown.
 */
public class DuplicateKeyYamlConstructor extends SafeConstructor {
  @Setter
  private String fileName;
  /**
   * Constructs a new {@code DuplicateKeyYamlConstructor} with the default
   * {@link LoaderOptions}.
   */
  public DuplicateKeyYamlConstructor() {
    super(new LoaderOptions());
    this.fileName = "";
  }

  /**
   * Constructs a mapping from the provided {@link MappingNode}, ensuring that
   * no duplicate keys are present. If a duplicate key is found, this method
   * throws a {@link YAMLException}.
   *
   * @param node the {@code MappingNode} to process
   * @return the constructed {@code Map<Object, Object>} from the YAML node
   * @throws YAMLException if a duplicate key is found
   */
  @Override
  protected Map<Object, Object> constructMapping(MappingNode node) {
    Set<Object> keys = new HashSet<>();
    for (NodeTuple tuple : node.getValue()) {
      Object key = constructObject(tuple.getKeyNode());
      Node keyNode = tuple.getKeyNode();
      int lineNumber = keyNode.getStartMark().getLine() + 1; // +1 to make it 1-indexed
      int columnNumber = keyNode.getStartMark().getColumn() + 1; // +1 to make it 1-indexed
      if (!keys.add(key)) {
        throw new YAMLException(
          String.format("%s:%d:%d: Error: Duplicate key %s found in YAML file.", this.fileName,
            lineNumber, columnNumber, key)
        );
      }
    }
    return super.constructMapping(node);
  }
}
