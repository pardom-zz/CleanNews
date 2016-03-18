package clean.news.usecase

import rx.Observable

class Strategy(private val flag: Int) {
	val useDisk: Boolean by lazy { flag and DISK != 0 }
	val useMemory: Boolean by lazy { flag and MEMORY != 0 }
	val useNetwork: Boolean by lazy { flag and NETWORK != 0 }

	fun <T> execute(
			diskObservable: Observable<T>,
			memoryObservable: Observable<T>,
			networkObservable: Observable<T>,
			save: (T) -> Any?): Observable<T> {

		val observables = mutableListOf<Observable<T>>()
		val network = networkObservable.doOnNext { save(it) }
		val memory = memoryObservable.single().onErrorResumeNext(Observable.never()).takeUntil(network)
		val disk = diskObservable.single().onErrorResumeNext(Observable.never()).takeUntil(memory)

		if (useMemory) observables.add(memory)
		if (useDisk) observables.add(disk)
		if (useNetwork) observables.add(network)

		return Observable.merge(observables)
	}

	companion object {
		const val DISK = 1
		const val MEMORY = 2
		const val NETWORK = 4

		const val WARM = DISK or MEMORY or NETWORK
	}
}
