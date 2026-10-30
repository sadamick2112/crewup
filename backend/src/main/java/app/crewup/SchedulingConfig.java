package app.crewup;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Turns on {@code @Scheduled} jobs, such as the daily purge of deleted crews (M1-7). */
@Configuration
@EnableScheduling
class SchedulingConfig {}
