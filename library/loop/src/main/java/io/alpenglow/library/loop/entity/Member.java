package io.alpenglow.library.loop.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "member")
public class Member {
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  @Column(name = "id", nullable = false)
  public UUID id;

  @Column(name = "first_name", nullable = false)
  public String firstName;

  @Column(name = "middle_name")
  public String middleName;

  @Column(name = "last_name", nullable = false)
  public String lastName;
}
