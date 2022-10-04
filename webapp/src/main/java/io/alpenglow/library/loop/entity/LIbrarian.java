package io.alpenglow.library.loop.entity;

import jakarta.persistence.Entity;

import java.util.UUID;

@jakarta.persistence.Entity
@jakarta.persistence.Table(name = "l_ibrarian")
@Entity
public class LIbrarian {
  @jakarta.persistence.Id
  @jakarta.persistence.GeneratedValue(strategy = jakarta.persistence.GenerationType.AUTO)
  @jakarta.persistence.Column(name = "id", nullable = false)
  private UUID id;

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

}
