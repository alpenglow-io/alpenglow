package io.alpenglow.library.loop.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "lendings")
public class Lending {
  public UUID id;
  public LocalDate lendedAt;
  public LocalDate dueAt;

  @ManyToOne
  @JoinColumn(name = "book_id")
  public Book book;

  @ManyToOne
  @JoinColumn(name = "member_id")
  public Member member;
}
