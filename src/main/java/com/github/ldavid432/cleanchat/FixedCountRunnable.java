package com.github.ldavid432.cleanchat;

import java.util.concurrent.CancellationException;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;

@Slf4j
class FixedCountRunnable implements Runnable
{

	private final Consumer<Runnable> command;
	private final int totalExecutionCount;
	private final Runnable onFailure;

	private int executionCount = 0;

	FixedCountRunnable(Consumer<Runnable> command, int totalExecutionCount, Runnable onFailure)
	{
		this.command = command;
		this.totalExecutionCount = totalExecutionCount;
		this.onFailure = onFailure;
	}

	@Override
	public void run()
	{
		command.accept(this::cancel);

		executionCount++;

		if (executionCount >= totalExecutionCount)
		{
			onFailure.run();
			cancel();
		}
	}

	private void cancel() {
		throw new CancellationException();
	}
}
