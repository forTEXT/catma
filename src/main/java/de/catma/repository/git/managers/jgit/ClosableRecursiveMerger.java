package de.catma.repository.git.managers.jgit;

import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.merge.RecursiveMerger;

import java.util.logging.Level;
import java.util.logging.Logger;

public class ClosableRecursiveMerger extends RecursiveMerger implements AutoCloseable {
    public ClosableRecursiveMerger(Repository local, boolean inCore) {
        super(local, inCore);
    }

    public void close() {
        getRepository().getObjectDatabase().close();

        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            try {
                Thread.sleep(2500);
            }
            catch (InterruptedException e) {
                Logger.getLogger(getClass().getName()).log(
                        Level.WARNING,
                        "Closing sleep was interrupted",
                        e
                );
            }
        }
    }
}
