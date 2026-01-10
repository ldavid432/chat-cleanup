# Clean Chat ![Install Count](https://img.shields.io/endpoint?url=https://api.runelite.net/pluginhub/shields/installs/plugin/clean-chat) ![Rank](https://img.shields.io/endpoint?url=https://api.runelite.net/pluginhub/shields/rank/plugin/clean-chat)

All the below options are toggleable (most are **off** by default)

- Misc
  - Remove 'Welcome to RuneScape' message
  - Adjust next line indentation
    - Several options to reduce the indentation on the following lines of multi-line messages
  - Hide the chat scrollbar
    - WHen hidden you can still scroll with your mouse wheel
  - Set a custom name for each channel
    - Intended to help you reduce the channel name size instead of full removal, but can also be customized to anything you want
- Clan
  - Remove 'To talk in your clan's channel...' message
  - Remove clan name from messages
- Guest Clan
  - Remove guest clan 'To talk...' message
  - Remove guest clan 'Attempting to reconnect...' message
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

### Custom channel name

- To reference your current clan name use `$$`
- To add color wrap whatever you want to color with: `<col=123456>` and `</col>` where `123456` is a hex color code
  - Example (blue): `[<col=0000ff>My Clan</col>]`
- Can also be used to remove the brackets around your clan name if you so desire
- To disable either leave blank or reset to the default value (right-click the specific config name)

## Known Issues

- Setting an indentation mode other than the default (`Message`) may shift multi-line messages to the right by one or two pixels
- After hopping clan/gim broadcasts don't have a channel for a few seconds
  - Unfortunately this seems to just be a Chat History plugin or maybe even Jagex bug where the channel is not populated until you actually reconnect to it
  - I may look at fixing this as a new "feature" in the future, but it is not caused by the Clean Chat plugin
