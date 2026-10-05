package com.swl.baoaiagent.agent;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 结构化 SSE 事件，用于向前端下发可读的执行过程。
 *
 * type 取值：tool_call | tool_result | answer | notice | error | done
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AgentEvent(String type, Integer step, String tool, String label,
                         String text, String detail, Boolean truncated) {
}
