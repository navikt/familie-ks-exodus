package no.nav.historisk.innsyn.testutil.oracle

import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext

class OracleContainerTruncateExtension : BeforeEachCallback, AfterAllCallback {
    override fun beforeEach(context: ExtensionContext) {
        OracleContainerInitializer.truncate()
    }

    override fun afterAll(context: ExtensionContext) {
        OracleContainerInitializer.truncate()
    }
}