# Clean Chat

All the below options are toggleable

- Misc
  - Remove 'Welcome to RuneScape' message
- Clan
  - Remove 'To talk in your clan's channel...' message
  - Remove clan name from messages
- Guest Clan
  - Remove guest clan 'To talk...' message
  - Remove guest clan name from messages
- Friend's Chat
  - Remove 'To talk, start each line of chat with the / symbol.' message
  - Remove friends chat name from messages
  - Remove 'Attempting to join chat-channel...' message
  - Remove 'Now talking in chat-channel...' message
- GIM chat
  - Remove 'To talk in your Ironman Group's channel...' message
  - Remove group name from messages
  - Remove group broadcasts from the clan chat tab (only display them in group tab)

## Known Issues

- All chats should continue to respect your selected chat color (either from the `Chat Colors` built-in plugin or OSRS settings).
However, when changed, colors will not be updated in older messages.
  - If you have the `Chat History` plugin enabled then you can re-log to refresh all the messages.

- If `Remove GIM name` is disabled and `Move GIM broadcasts` is enabled, and then `Remove GIM name` is later enabled any broadcasts will still contain the group name.
  Can be fixed by re-logging.

#### World Hopping / Re-Logging with the `Chat History` plugin enabled

- When enabling the plugin or re-logging or hopping worlds (with any of the remove channel name options enabled) the ordering of the `All` chat tab will become incorrect with all the channel chats being moved to the bottom.
  The respective channel tabs ordering and timestamps should remain correct though.

- You'll also notice it takes a few seconds for messages to be cleaned after re-logging/hopping.
  This is because we have to wait for your client to connect to each channel so that we can retrieve icon information

- If you aren't in a channel anymore, but you still have that chat channel's messages in your history then those chats will not be 'cleaned' since your client never connects to the channel to trigger the cleaning.

#### Disabling the plugin

- Disabling the plugin does not "un-clean" previous chat messages, although any new ones will obviously act like normal.
  - If you have the `Chat History` plugin enabled you can simply re-log after disabling and any previous messages will show up as normal again.

- If you disable the plugin and have remove clan chat name enabled you'll notice that all your previous clan messages now have an 'Accept challenge' option.
  This is just an unfortunate side effect of how the clan name is removed.
  - The challenge shouldn't even work but if you want to remove that simply re-log after disabling the plugin.
