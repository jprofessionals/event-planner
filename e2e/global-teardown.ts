export default async function globalTeardown() {
	const backendProcess = globalThis.__backendProcess;
	const frontendProcess = globalThis.__frontendProcess;
	const pgContainer = globalThis.__pgContainer;

	if (backendProcess) backendProcess.kill();
	if (frontendProcess) frontendProcess.kill();
	if (pgContainer) await pgContainer.stop();
}
