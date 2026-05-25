package com.dnd.data.dto;

import com.dnd.model.item.books.Book;

import java.util.List;

public class BookCatalog {
    private List<Book> books;

    public BookCatalog() {
    }

    public List<Book> getBooks() {
        return books;
    }

    public void setBooks(List<Book> books) {
        this.books = books;
    }
}

