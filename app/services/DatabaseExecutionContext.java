package services;

import javax.inject.Inject;

import akka.actor.ActorSystem;
import play.api.libs.concurrent.CustomExecutionContext;

/**
 * @author sriva
 *
 */
public class DatabaseExecutionContext extends CustomExecutionContext {

	@Inject
	public DatabaseExecutionContext(ActorSystem actorSystem) {
		super(actorSystem, "play.db");
	}
}
