package io.alpenglow.library.loop.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.UUID;

import static jakarta.persistence.CascadeType.PERSIST;
import static jakarta.persistence.GenerationType.AUTO;

@Entity
@Table(name = "librarians")
public class Librarian {
  @Id
  @GeneratedValue(strategy = AUTO)
  @Column(name = "id", nullable = false)
  public UUID id;

  @Column(name = "name", nullable = false, unique = true)
  public String name;

  @ManyToOne(cascade = PERSIST)
  @JoinColumn(name = "library_id")
  public Library library;
}
