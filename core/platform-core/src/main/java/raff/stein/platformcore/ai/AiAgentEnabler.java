package raff.stein.platformcore.ai;

import java.util.List;

/**
 * SPI used by business microservices to expose their LangChain4j tools.
 * <p>
 * Tool beans returned by {@link #tools()} are typically discovered and passed to a service-specific agent
 * factory (for example {@code AiServices.builder(...).tools(...)}).
 */
public interface AiAgentEnabler {

    /**
     * @return the list of tool beans to be registered for an agent.
     */
    List<Object> tools();
}

