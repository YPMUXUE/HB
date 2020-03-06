package priv.common.connection;

/**
 *  * @author  pyuan
 *  * @date    2020/3/6 0006
 *  * @Description
 *  *
 *  
 */
public interface RemoteDataHandler<T> {
	void receiveData(T data);

}
