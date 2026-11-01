package com.example.fileindexer.shell;

import com.example.fileindexer.lucene.LuceneStore;
import com.example.fileindexer.model.Drive;
import com.example.fileindexer.model.IndexSummary;
import com.example.fileindexer.model.SearchHit;
import com.example.fileindexer.model.SearchType;
import com.example.fileindexer.service.DriveService;
import com.example.fileindexer.service.IndexingService;
import java.util.List;
import org.jline.reader.UserInterruptException;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStyle;
import org.springframework.shell.core.command.annotation.Command;
import org.springframework.shell.core.command.annotation.Option;
import org.springframework.stereotype.Component;

/** Registered through {@code @EnableCommand} on the application class. */
@Component
public class IndexerCommands {

    private final DriveService drives;
    private final IndexingService indexing;
    private final LuceneStore lucene;
    private final Prompter prompter;

    public IndexerCommands(DriveService drives, IndexingService indexing, LuceneStore lucene, Prompter prompter) {
        this.drives = drives;
        this.indexing = indexing;
        this.lucene = lucene;
        this.prompter = prompter;
    }

    @Command(name = "index", description = "Index new or changed files of the selected drives")
    public void index(
            @Option(longName = "drives", shortName = 'd',
                    description = "Drives to index, e.g. C,D or 'all' (asked interactively if omitted)")
            String drivesOption) {
        try {
            List<Drive> selected = prompter.selectDrives(drives.listDrives(), drivesOption);
            IndexSummary summary = indexing.index(selected, prompter::println);
            prompter.println("Finished: " + IndexingService.describe(summary));
        } catch (UserInterruptException e) {
            prompter.println("Cancelled.");
        } catch (Exception e) {
            prompter.println("Indexing failed: " + e.getMessage());
        }
    }

    @Command(name = "search", description = "Search the index: choose type, drives, then enter the text")
    public void search(
            @Option(longName = "type", shortName = 't', description = "regular or regex") String typeOption,
            @Option(longName = "drives", shortName = 'd', description = "Drives to search, e.g. C,D or 'all'")
            String drivesOption,
            @Option(longName = "text", shortName = 'q', description = "Text or regex to search for")
            String textOption) {
        try {
            SearchType type = prompter.selectSearchType(typeOption);
            List<Drive> selected = prompter.selectDrives(drives.listDrives(), drivesOption);
            String text = prompter.askText(textOption);

            List<SearchHit> hits = lucene.search(type, text, selected.stream().map(Drive::id).toList());
            if (hits.isEmpty()) {
                prompter.println(new AttributedString("No matches. (Has 'index' been run for these drives?)",
                        AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN)).toAnsi());
                return;
            }
            prompter.println("%d match(es):".formatted(hits.size()));
            int n = 1;
            for (SearchHit hit : hits) {
                prompter.println(new AttributedString("%3d. %s   (score %.2f)".formatted(n++, hit.path(), hit.score()),
                        AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN)).toAnsi());
            }
        } catch (UserInterruptException e) {
            prompter.println("Cancelled.");
        } catch (IllegalArgumentException e) {
            prompter.println("Invalid search: " + e.getMessage());
        } catch (Exception e) {
            prompter.println("Search failed: " + e.getMessage());
        }
    }
}
