package priv.common.events;

/**
 *  * @author  pyuan
 *  * @date    2019/8/27 0027
 *  * @Description
 *  *
 *  
 */
public class ConnectFailedProxyEvent implements ProxyEvent {
	private final Throwable e;

	public ConnectFailedProxyEvent(Throwable e) {
		this.e = e;
	}

	public Throwable getCause() {
		return e;
	}
}
