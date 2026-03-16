package com.electromart;

import java.util.List;
import java.util.Map;

public record AggregateResult(String mode, List<Map<String, Object>> rows) {
}
