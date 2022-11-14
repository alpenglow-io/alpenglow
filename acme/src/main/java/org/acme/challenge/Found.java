package org.acme.challenge;

import org.acme.Challenge;
import org.shredzone.acme4j.Authorization;

public record Found(Authorization authorization) implements Challenge {
}
