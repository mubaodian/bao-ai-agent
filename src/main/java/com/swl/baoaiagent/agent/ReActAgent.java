package com.swl.baoaiagent.agent;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

@EqualsAndHashCode(callSuper = true)
@Data
@Slf4j
public abstract class ReActAgent extends BaseAgent {

    /**
     * 处理当前状态并决定下一步行动
     * @return 是否需要执行行动，true表示需要行动，false表示无需行动
     */
    public abstract boolean think();

    /**
     * 执行决定的行动
     * @return 行动结果
     */
    public abstract String act();

    @Override
    public String step() {
        try {
            //先思考
            boolean shouldAct = think();
            if(!shouldAct){
                return "思考完成，无需行动";
            }
            //再行动
            return act();
        } catch (Exception e) {
            e.printStackTrace();
            return "步骤执行失败：" + e.getMessage();
        }
    }
}
