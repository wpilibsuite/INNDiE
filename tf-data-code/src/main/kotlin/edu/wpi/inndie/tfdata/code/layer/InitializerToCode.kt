package edu.wpi.inndie.tfdata.code.layer

import arrow.core.Either
import edu.wpi.inndie.tfdata.layer.Initializer

interface InitializerToCode {

    /**
     * Get the code to make a new instance of an [initializer].
     *
     * @param initializer The [Initializer].
     * @return The code to make a new instance of the [initializer].
     */
    fun makeNewInitializer(initializer: Initializer): Either<String, String>
}
