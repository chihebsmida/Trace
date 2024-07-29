package org.epac.trace.dto;

import java.time.Duration;

public record WorkSummary(Duration workDuration, Duration pauseDuration, Duration inactiveDuration) { }
