/**
 * Cross-cutting types every module may use, such as {@link app.crewup.shared.Ids}. Open, so other modules can use
 * its types without it publishing a named interface. Keep it small: domain code belongs in its own module.
 */
@ApplicationModule(type = ApplicationModule.Type.OPEN)
package app.crewup.shared;

import org.springframework.modulith.ApplicationModule;
