package com.dnd.data.dto;

import com.dnd.model.world.Language;

import java.util.List;

public class LanguageCatalog {
    private List<Language> languages;

    public LanguageCatalog() {
    }

    public List<Language> getLanguages() {
        return languages;
    }

    public void setLanguages(List<Language> languages) {
        this.languages = languages;
    }
}

