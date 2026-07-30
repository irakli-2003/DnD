package com.dnd.model.world;

import com.dnd.model.interfaces.Printable;
import com.dnd.model.item.books.Book;

import java.util.Map;

public class Language implements Printable {
    private String id;
    private String name;
    private Map<String, String> dictionary;
    private Book requiredMaterial;
    private int requiredLongRests;

    public Language() {
    }

    public Language(String id, String name, Map<String, String> dictionary, Book requiredMaterial, int requiredLongRests) {
        this.id = id;
        this.name = name;
        this.dictionary = dictionary;
        this.requiredMaterial = requiredMaterial;
        this.requiredLongRests = requiredLongRests;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, String> getDictionary() {
        return dictionary;
    }

    public void setDictionary(Map<String, String> dictionary) {
        this.dictionary = dictionary;
    }

    public Book getRequiredMaterial() {
        return requiredMaterial;
    }

    public void setRequiredMaterial(Book requiredMaterial) {
        this.requiredMaterial = requiredMaterial;
    }

    public int getRequiredLongRests() {
        return requiredLongRests;
    }

    public void setRequiredLongRests(int requiredLongRests) {
        this.requiredLongRests = requiredLongRests;
    }

    @Override
    public String toString() {
        return name != null ? name : id;
    }
}

