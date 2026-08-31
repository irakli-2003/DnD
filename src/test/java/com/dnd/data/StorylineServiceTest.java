package com.dnd.data;

import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class StorylineServiceTest {

    private StorylineService newService() throws IOException {
        Path campaignRoot = Files.createTempDirectory("dnd-storyline");
        StorylineService service = new StorylineService(campaignRoot);
        service.ensureRoot();
        return service;
    }

    @Test
    public void ensureRootCreatesStorylineFolder() throws IOException {
        StorylineService service = newService();
        assertTrue(Files.isDirectory(service.getRoot()));
    }

    @Test
    public void createFolderAndFileAppearAsChildren() throws IOException {
        StorylineService service = newService();
        Path arc1 = service.createFolder(service.getRoot(), "Arc 1");
        Path session1 = service.createFile(arc1, "session-1.txt");

        List<Path> rootChildren = service.listChildren(service.getRoot());
        assertEquals(1, rootChildren.size());
        assertEquals(arc1, rootChildren.get(0));

        List<Path> arcChildren = service.listChildren(arc1);
        assertEquals(1, arcChildren.size());
        assertEquals(session1, arcChildren.get(0));
        assertTrue(service.isSessionFile(session1));
        assertTrue(service.isFolder(arc1));
    }

    @Test
    public void listChildrenOrdersFoldersBeforeFilesAlphabetically() throws IOException {
        StorylineService service = newService();
        service.createFile(service.getRoot(), "b.txt");
        service.createFolder(service.getRoot(), "a-folder");
        service.createFile(service.getRoot(), "a.txt");

        List<Path> children = service.listChildren(service.getRoot());
        assertEquals(3, children.size());
        assertEquals("a-folder", children.get(0).getFileName().toString());
        assertEquals("a.txt", children.get(1).getFileName().toString());
        assertEquals("b.txt", children.get(2).getFileName().toString());
    }

    @Test(expected = IllegalArgumentException.class)
    public void createFileRejectsBlankName() throws IOException {
        StorylineService service = newService();
        service.createFile(service.getRoot(), "  ");
    }

    @Test(expected = IllegalArgumentException.class)
    public void createFileRejectsDuplicateName() throws IOException {
        StorylineService service = newService();
        service.createFile(service.getRoot(), "session.txt");
        service.createFile(service.getRoot(), "session.txt");
    }

    @Test
    public void readWriteTextRoundTrips() throws IOException {
        StorylineService service = newService();
        Path file = service.createFile(service.getRoot(), "notes.txt");
        service.writeText(file, "The party enters the tavern.");
        assertEquals("The party enters the tavern.", service.readText(file));
    }

    @Test
    public void deleteRemovesFolderRecursively() throws IOException {
        StorylineService service = newService();
        Path folder = service.createFolder(service.getRoot(), "to-delete");
        service.createFile(folder, "a.txt");
        service.createFile(folder, "b.txt");

        service.delete(folder);

        assertFalse(Files.exists(folder));
        assertTrue(service.listChildren(service.getRoot()).isEmpty());
    }

    @Test
    public void moveRelocatesFileToDestinationFolder() throws IOException {
        StorylineService service = newService();
        Path file = service.createFile(service.getRoot(), "session.txt");
        Path destination = service.createFolder(service.getRoot(), "arc");

        Path moved = service.move(file, destination);

        assertFalse(Files.exists(file));
        assertTrue(Files.exists(moved));
        assertEquals(destination, moved.getParent());
    }

    @Test
    public void moveRelocatesFolderWithChildren() throws IOException {
        StorylineService service = newService();
        Path source = service.createFolder(service.getRoot(), "arc-1");
        Path child = service.createFile(source, "session-1.txt");
        Path destination = service.createFolder(service.getRoot(), "arc-2");

        Path moved = service.move(source, destination);

        assertTrue(Files.isDirectory(moved));
        assertTrue(Files.exists(moved.resolve(child.getFileName())));
    }

    @Test(expected = IllegalArgumentException.class)
    public void moveRejectsMovingFolderIntoItself() throws IOException {
        StorylineService service = newService();
        Path folder = service.createFolder(service.getRoot(), "arc-1");
        service.move(folder, folder);
    }

    @Test(expected = IllegalArgumentException.class)
    public void moveRejectsMovingFolderIntoOwnDescendant() throws IOException {
        StorylineService service = newService();
        Path parent = service.createFolder(service.getRoot(), "parent");
        Path child = service.createFolder(parent, "child");
        service.move(parent, child);
    }

    @Test(expected = IllegalArgumentException.class)
    public void moveRejectsDuplicateNameAtDestination() throws IOException {
        StorylineService service = newService();
        Path source = service.createFile(service.getRoot(), "session.txt");
        Path destination = service.createFolder(service.getRoot(), "arc");
        service.createFile(destination, "session.txt");

        service.move(source, destination);
    }

    @Test
    public void childrenDefaultToFoldersFirstThenAlphabetical() throws IOException {
        StorylineService service = newService();
        service.createFile(service.getRoot(), "b-session.txt");
        service.createFile(service.getRoot(), "a-session.txt");
        service.createFolder(service.getRoot(), "z-arc");

        assertEquals(List.of("z-arc", "a-session.txt", "b-session.txt"), names(service, service.getRoot()));
    }

    @Test
    public void reorderMovesChildToRequestedIndexAndPersists() throws IOException {
        StorylineService service = newService();
        Path first = service.createFile(service.getRoot(), "a.txt");
        service.createFile(service.getRoot(), "b.txt");
        service.createFile(service.getRoot(), "c.txt");

        service.reorder(first, 2);
        assertEquals(List.of("b.txt", "c.txt", "a.txt"), names(service, service.getRoot()));

        // A fresh service instance must see the same order: it is stored on disk, not in memory.
        StorylineService reopened = new StorylineService(service.getRoot().getParent());
        assertEquals(List.of("b.txt", "c.txt", "a.txt"), names(reopened, reopened.getRoot()));
    }

    @Test
    public void moveUpAndMoveDownShiftBySinglePosition() throws IOException {
        StorylineService service = newService();
        service.createFile(service.getRoot(), "a.txt");
        Path middle = service.createFile(service.getRoot(), "b.txt");
        service.createFile(service.getRoot(), "c.txt");

        service.moveUp(middle);
        assertEquals(List.of("b.txt", "a.txt", "c.txt"), names(service, service.getRoot()));

        service.moveDown(middle);
        assertEquals(List.of("a.txt", "b.txt", "c.txt"), names(service, service.getRoot()));
    }

    @Test
    public void moveUpAtTopAndMoveDownAtBottomAreNoOps() throws IOException {
        StorylineService service = newService();
        Path first = service.createFile(service.getRoot(), "a.txt");
        Path last = service.createFile(service.getRoot(), "b.txt");

        service.moveUp(first);
        service.moveDown(last);

        assertEquals(List.of("a.txt", "b.txt"), names(service, service.getRoot()));
    }

    @Test
    public void newItemsAppearAfterExplicitlyOrderedOnes() throws IOException {
        StorylineService service = newService();
        Path a = service.createFile(service.getRoot(), "a.txt");
        service.createFile(service.getRoot(), "b.txt");
        service.reorder(a, 1);
        assertEquals(List.of("b.txt", "a.txt"), names(service, service.getRoot()));

        service.createFile(service.getRoot(), "c.txt");
        assertEquals(List.of("b.txt", "a.txt", "c.txt"), names(service, service.getRoot()));
    }

    @Test
    public void orderFileIsNotListedAsAChild() throws IOException {
        StorylineService service = newService();
        Path a = service.createFile(service.getRoot(), "a.txt");
        service.createFile(service.getRoot(), "b.txt");
        service.reorder(a, 1);

        assertFalse(names(service, service.getRoot()).contains(StorylineService.ORDER_FILE));
    }

    @Test
    public void indexOfReportsPositionAmongSiblings() throws IOException {
        StorylineService service = newService();
        service.createFile(service.getRoot(), "a.txt");
        Path b = service.createFile(service.getRoot(), "b.txt");

        assertEquals(1, service.indexOf(b));
    }

    private List<String> names(StorylineService service, Path folder) {
        List<String> out = new ArrayList<>();
        for (Path child : service.listChildren(folder)) {
            out.add(child.getFileName().toString());
        }
        return out;
    }
}
