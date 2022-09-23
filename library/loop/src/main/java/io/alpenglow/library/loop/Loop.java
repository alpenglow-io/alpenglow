package io.alpenglow.library.loop;

import java.time.LocalDate;
import java.util.List;

public interface Loop {
  class Library {
    private String name;
    private String address;
    private Catalog catalog;
    private List<Librarian> librarians;
    private List<Member> members;
  }

  class Catalog {
    private List<Book> books;

    public List<Book> search(String query) {
      return List.of();
    }
    public BookItem addBookItem(Librarian librarian, BookItem bookItem) {
      return null;
    }
  }

  class Book {
    private String id;
    private String title;
    private List<Author> authors;
  }

  class Author {
    private String id;
    private String fullName;
    private List<Book> books;
  }

  class BookItem {
    private String id;
    private String libraryId;

    public BookLending checkout(Member member) {
      return null;
    }
  }

  class Librarian extends User {
    public boolean block(Member member) {
      return false;
    }
    public boolean unblock(Member member) {
      return false;
    }
    public BookItem add(BookItem bookItem) {
      return null;
    }
    public List<BookLending> findBookLendingsBy(Member member) {
      return List.of();
    }
  }

  class Member extends User {
    public boolean isBlocked() { return false; }
    public boolean block() { return false; }
    public boolean unblock() { return false; }
    public boolean returns(BookLending bookLending) {return false;}
    public BookLending checkout(BookItem bookItem) {return null;}
  }

  class User {
    private String id;
    private String email;
    private String password;
    public boolean login() { return false;}
  }

  class BookLending {
    private String id;
    private LocalDate lendingAt;
    private LocalDate dueAt;
    public boolean isLate() { return false;}
    public boolean returnBook() { return false; }
  }
}
