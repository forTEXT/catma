package de.catma.repository.git.graph.interfaces;

import java.io.IOException;
import java.util.Map;

public interface DocumentIndexProvider {
    Map get(String documentId) throws IOException;
}
