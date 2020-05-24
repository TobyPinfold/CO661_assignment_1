```/**-----------------------------------------------------------------------------------------------**
 *         ,-----.                                                                                 *
 *        '  .--./ ,---. ,--,--,  ,---.,--.,--.,--.--.,--.--. ,---. ,--,--,  ,---.,--. ,--.        *
 *        |  |    | .-. ||      \| .--'|  ||  ||  .--'|  .--'| .-. :|      \| .--' \  '  /         *
 *        '  '--'\' '-' '|  ||  |\ `--.'  ''  '|  |   |  |   \   --.|  ||  |\ `--.  \   '          *
 *         `-----' `---' `--''--' `---' `----' `--'   `--'    `----'`--''--' `---'.-'  /           *
 *                                  Shared FileSystem Explanation                 `---'            *
 *                                                                                                 *
 **-----------------------------------------------------------------------------------------------**
 *
 *  My implementation for the filesystem is using a single semaphore to all process', the
 *  semaphore is initialised with a permit count of maximum integer to allow for a maximum
 *  amount of reader processes to run without blocking one another. The Read action only requires
 *  a single permit to execute and so many read can happen at any given time. A Write action
 *  however requires all permits inorder to proceed. Therefore a write cannot attain the semaphore
 *  until all permits are free, and if it does indeed get all permits, then a read action cannot
 *  acquire a permit to process either. This assures mutual exclusion between the two actions being
 *  performed and prevents race conditions between the two.
 *
 *  Fairness is enabled when I initialise the Semaphore as it uses FIFO on its queue of threads
 *  trying to acquire permits. This ensures that all threads get access to the Semaphore which
 *  prevents any thread being being blocked indefinitely by other processes causing the process to
 *  become starved.
 *
 *  With regards to write operations, the semaphore using FIFO ensures write can acquire available
 *  permits in queue and will have a chance to run. The necessity to acquire all permits then also
 *  ensures mutual exclusion in that no read operations can perform until write has released its
 *  permits (given it acquires all available permits) and Write cannot perform until it has
 *  acquired all permits, meaning all read actions must have finished before it can acquire
 *
 **-----------------------------------------------------------------------------------------------**/
