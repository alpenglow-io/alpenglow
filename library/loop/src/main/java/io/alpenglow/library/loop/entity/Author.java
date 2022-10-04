package io.alpenglow.library.loop.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

import java.util.List;
import java.util.UUID;

import static jakarta.persistence.CascadeType.PERSIST;
import static jakarta.persistence.GenerationType.AUTO;

@Entity
@Table(name = "author")
public class Author {
  @Id
  @GeneratedValue(strategy = AUTO)
  @Column(name = "id", nullable = false)
  private UUID id;

  @Column(name = "full_name", nullable = false)
  private String fullName;

  @ManyToMany(mappedBy = "authors", targetEntity = Book.class, cascade = PERSIST)
  public List<Book> books;
}
