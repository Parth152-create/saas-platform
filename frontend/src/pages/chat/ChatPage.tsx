import React, { useEffect, useRef, useState } from 'react';
import {
  AtSign,
  ChevronLeft,
  FolderKanban,
  Hash,
  MessageSquare,
  Plus,
  Send,
  User,
  Users,
} from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import {
  chatApi,
  type ChatChannel,
  type ChatMessage,
  type DirectConversation,
  type DirectMessage,
} from '../../api/chatApi';
import { usersApi } from '../../api/usersApi';
import { hrmApi } from '../../api/hrmApi';
import type { WorkspaceUser, Employee } from '../../api/types';
import { wsManager } from '../../collaboration/websocketClient';
import { Button } from '../../components/common/Button';
import { Badge } from '../../components/common/Badge';
import { Input } from '../../components/common/Input';
import { Modal } from '../../components/common/Modal';
import { LoadingSkeleton } from '../../components/common/LoadingSkeleton';

type ActiveChat =
  | { type: 'channel'; channel: ChatChannel }
  | { type: 'dm'; conversation: DirectConversation };

export const ChatPage: React.FC = () => {
  const { user, hasRole } = useAuth();

  const [channels, setChannels] = useState<ChatChannel[]>([]);
  const [directConvos, setDirectConvos] = useState<DirectConversation[]>([]);
  const [activeChat, setActiveChat] = useState<ActiveChat | null>(null);

  const [channelMessages, setChannelMessages] = useState<ChatMessage[]>([]);
  const [directMessages, setDirectMessages] = useState<DirectMessage[]>([]);

  const [workspaceUsers, setWorkspaceUsers] = useState<WorkspaceUser[]>([]);
  const [employees, setEmployees] = useState<Employee[]>([]);

  const [messageInput, setMessageInput] = useState('');
  const [isLoadingList, setIsLoadingList] = useState(true);
  const [isLoadingMessages, setIsLoadingMessages] = useState(false);
  const [isSending, setIsSending] = useState(false);

  // New Channel Modal
  const [isNewChannelOpen, setIsNewChannelOpen] = useState(false);
  const [newChannelName, setNewChannelName] = useState('');
  const [newChannelTopic, setNewChannelTopic] = useState('');
  const [channelError, setChannelError] = useState<string | null>(null);

  // New DM Modal
  const [isNewDmOpen, setIsNewDmOpen] = useState(false);
  const [dmSearchQuery, setDmSearchQuery] = useState('');

  // Mobile view state
  const [showMobileChat, setShowMobileChat] = useState(false);
  const [isConnected, setIsConnected] = useState(false);

  useEffect(() => {
    wsManager.connect();
    const unsub = wsManager.onConnectionChange(setIsConnected);
    return () => unsub();
  }, []);

  const messagesEndRef = useRef<HTMLDivElement>(null);

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  // Load Channels & Direct Conversations
  useEffect(() => {
    let isMounted = true;
    Promise.all([
      chatApi.getChannels(),
      chatApi.getDirectConversations(),
    ])
      .then(([channelList, convoList]) => {
        if (!isMounted) return;
        setChannels(channelList);
        setDirectConvos(convoList);
        if (channelList.length > 0) {
          const general = channelList.find((c) => c.name === 'general') || channelList[0];
          setActiveChat((prev) => prev || { type: 'channel', channel: general });
        }
      })
      .catch((err) => {
        console.error('Failed to load chat sidebar data:', err);
      })
      .finally(() => {
        if (isMounted) setIsLoadingList(false);
      });

    return () => {
      isMounted = false;
    };
  }, []);

  // Load users for new DM
  useEffect(() => {
    if (isNewDmOpen) {
      Promise.all([usersApi.getUsers(), hrmApi.getEmployees()])
        .then(([uList, empList]) => {
          setWorkspaceUsers(uList.filter((u) => u.status === 'ACTIVE' && u.id !== user?.id));
          setEmployees(empList);
        })
        .catch((err) => console.error('Failed to load users for DM:', err));
    }
  }, [isNewDmOpen, user?.id]);

  // Load Messages for Active Chat
  useEffect(() => {
    if (!activeChat) return;

    let isMounted = true;

    if (activeChat.type === 'channel') {
      chatApi
        .getChannelMessages(activeChat.channel.id)
        .then((res) => {
          if (isMounted) {
            // Reverse so oldest is on top, newest at bottom
            const sorted = [...(res.content || [])].reverse();
            setChannelMessages(sorted);
            setIsLoadingMessages(false);
            setTimeout(scrollToBottom, 50);
          }
        })
        .catch((err) => {
          console.error('Failed to load channel messages:', err);
          if (isMounted) setIsLoadingMessages(false);
        });
    } else {
      chatApi
        .getDirectMessages(activeChat.conversation.id)
        .then((res) => {
          if (isMounted) {
            const sorted = [...(res.content || [])].reverse();
            setDirectMessages(sorted);
            setIsLoadingMessages(false);
            setTimeout(scrollToBottom, 50);
            // Mark convo as read locally
            setDirectConvos((prev) =>
              prev.map((c) =>
                c.id === activeChat.conversation.id ? { ...c, unreadCount: 0 } : c
              )
            );
          }
        })
        .catch((err) => {
          console.error('Failed to load DM messages:', err);
          if (isMounted) setIsLoadingMessages(false);
        });
    }

    return () => {
      isMounted = false;
    };
  }, [activeChat]);

  // WebSocket Subscription for Active Chat
  useEffect(() => {
    if (!activeChat) return;

    if (activeChat.type === 'channel') {
      const unsub = wsManager.subscribeToChannelMessage<ChatMessage>(
        activeChat.channel.id,
        (incoming) => {
          setChannelMessages((prev) => {
            if (prev.some((m) => m.id === incoming.id)) return prev;
            return [...prev, incoming];
          });
          setTimeout(scrollToBottom, 50);
        }
      );
      return () => unsub();
    } else {
      const unsub = wsManager.subscribeToDirectMessage<DirectMessage>(
        activeChat.conversation.id,
        (incoming) => {
          setDirectMessages((prev) => {
            if (prev.some((m) => m.id === incoming.id)) return prev;
            return [...prev, incoming];
          });
          setTimeout(scrollToBottom, 50);
        }
      );
      return () => unsub();
    }
  }, [activeChat]);

  // Handle Send Message
  const handleSendMessage = async (e?: React.FormEvent) => {
    if (e) e.preventDefault();
    if (!messageInput.trim() || !activeChat || isSending) return;

    const content = messageInput.trim();
    setMessageInput('');
    setIsSending(true);

    try {
      if (activeChat.type === 'channel') {
        const sent = await chatApi.sendChannelMessage(activeChat.channel.id, content);
        setChannelMessages((prev) => {
          if (prev.some((m) => m.id === sent.id)) return prev;
          return [...prev, sent];
        });
      } else {
        const sent = await chatApi.sendDirectMessage(activeChat.conversation.id, content);
        setDirectMessages((prev) => {
          if (prev.some((m) => m.id === sent.id)) return prev;
          return [...prev, sent];
        });
        // Update last message in sidebar
        setDirectConvos((prev) =>
          prev.map((c) =>
            c.id === activeChat.conversation.id
              ? { ...c, lastMessage: content, lastMessageAt: sent.createdAt }
              : c
          )
        );
      }
      setTimeout(scrollToBottom, 50);
    } catch (err) {
      console.error('Failed to send message:', err);
      // Restore input on failure
      setMessageInput(content);
    } finally {
      setIsSending(false);
    }
  };

  // Handle Create Channel
  const handleCreateChannel = async (e: React.FormEvent) => {
    e.preventDefault();
    setChannelError(null);
    const slug = newChannelName
      .trim()
      .toLowerCase()
      .replace(/[^a-z0-9-_]/g, '-');
    if (!slug) {
      setChannelError('Channel name is required');
      return;
    }

    try {
      const created = await chatApi.createChannel({
        name: slug,
        topic: newChannelTopic.trim() || undefined,
      });
      setChannels((prev) => [...prev, created]);
      setActiveChat({ type: 'channel', channel: created });
      setIsNewChannelOpen(false);
      setNewChannelName('');
      setNewChannelTopic('');
      setShowMobileChat(true);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Failed to create channel';
      setChannelError(msg);
    }
  };

  // Handle Start Direct Message
  const handleStartDm = async (targetUserId: string) => {
    try {
      const convo = await chatApi.getOrCreateDirectConversation(targetUserId);
      setDirectConvos((prev) => {
        const exists = prev.find((c) => c.id === convo.id);
        if (exists) return prev;
        return [convo, ...prev];
      });
      setActiveChat({ type: 'dm', conversation: convo });
      setIsNewDmOpen(false);
      setShowMobileChat(true);
    } catch (err) {
      console.error('Failed to open DM:', err);
    }
  };

  // Format Helpers
  const formatTime = (isoString: string) => {
    try {
      const d = new Date(isoString);
      return d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    } catch {
      return '';
    }
  };

  const getUserDisplayName = (u: WorkspaceUser) => {
    const emp = employees.find((e) => e.email.toLowerCase() === u.email.toLowerCase());
    return emp?.name || u.email.split('@')[0];
  };

  const highlightMentions = (content: string) => {
    const parts = content.split(/(@[a-zA-Z0-9._-]+)/g);
    return parts.map((part, i) => {
      if (part.startsWith('@')) {
        return (
          <span
            key={i}
            className="inline-block px-1.5 py-0.5 rounded bg-blue-50 dark:bg-blue-950/40 text-blue-600 dark:text-blue-400 font-semibold text-xs"
          >
            {part}
          </span>
        );
      }
      return part;
    });
  };

  return (
    <div className="flex h-[calc(100vh-6rem)] border border-neutral-200 dark:border-[#262626] rounded-xl overflow-hidden bg-white dark:bg-[#101010] shadow-xs">
      {/* ------------------------------------------------------------- */}
      {/* LEFT SIDEBAR: Channels & Direct Messages                      */}
      {/* ------------------------------------------------------------- */}
      <div
        className={`w-full md:w-80 shrink-0 border-r border-neutral-200 dark:border-[#262626] flex flex-col bg-neutral-50/70 dark:bg-[#141414] ${
          showMobileChat ? 'hidden md:flex' : 'flex'
        }`}
      >
        {/* Sidebar Header */}
        <div className="p-4 border-b border-neutral-200 dark:border-[#262626] flex items-center justify-between">
          <div className="flex items-center gap-2">
            <MessageSquare className="w-5 h-5 text-neutral-800 dark:text-neutral-200" />
            <h2 className="font-semibold text-sm text-neutral-900 dark:text-neutral-100">
              Collaboration
            </h2>
          </div>
          {hasRole('MANAGER') && (
            <Button
              variant="outline"
              size="sm"
              onClick={() => setIsNewChannelOpen(true)}
              leftIcon={<Plus className="w-3.5 h-3.5" />}
              className="text-xs h-7 px-2"
            >
              Channel
            </Button>
          )}
        </div>

        {/* Channels & DMs Scroll List */}
        <div className="flex-1 overflow-y-auto p-3 space-y-6">
          {isLoadingList ? (
            <div className="space-y-3 p-2">
              <LoadingSkeleton variant="text" rows={4} />
            </div>
          ) : (
            <>
              {/* Workspace Channels */}
              <div>
                <div className="flex items-center justify-between px-2 mb-1.5">
                  <span className="text-[11px] font-semibold tracking-wider text-neutral-400 uppercase">
                    Channels
                  </span>
                </div>
                <div className="space-y-0.5">
                  {channels.map((chan) => {
                    const isSelected =
                      activeChat?.type === 'channel' && activeChat.channel.id === chan.id;
                    return (
                      <button
                        key={chan.id}
                        onClick={() => {
                          setActiveChat({ type: 'channel', channel: chan });
                          setShowMobileChat(true);
                        }}
                        className={`w-full flex items-center gap-2 px-2.5 py-1.5 rounded-lg text-xs font-medium transition-colors text-left ${
                          isSelected
                            ? 'bg-neutral-900 text-white dark:bg-white dark:text-neutral-950 shadow-xs'
                            : 'text-neutral-700 hover:bg-neutral-200/60 dark:text-neutral-300 dark:hover:bg-[#202020]'
                        }`}
                      >
                        {chan.type === 'PROJECT' ? (
                          <FolderKanban className="w-3.5 h-3.5 shrink-0 opacity-70" />
                        ) : (
                          <Hash className="w-3.5 h-3.5 shrink-0 opacity-70" />
                        )}
                        <span className="truncate">{chan.name}</span>
                        {chan.type === 'PROJECT' && (
                          <span
                            className={`ml-auto text-[9px] px-1 py-0.2 rounded font-semibold uppercase ${
                              isSelected
                                ? 'bg-neutral-700 text-neutral-200 dark:bg-neutral-200 dark:text-neutral-800'
                                : 'bg-neutral-200 text-neutral-600 dark:bg-[#262626] dark:text-neutral-400'
                            }`}
                          >
                            Proj
                          </span>
                        )}
                      </button>
                    );
                  })}
                </div>
              </div>

              {/* Direct Messages */}
              <div>
                <div className="flex items-center justify-between px-2 mb-1.5">
                  <span className="text-[11px] font-semibold tracking-wider text-neutral-400 uppercase">
                    Direct Messages
                  </span>
                  <button
                    onClick={() => setIsNewDmOpen(true)}
                    className="text-neutral-400 hover:text-neutral-700 dark:hover:text-neutral-200 p-0.5 rounded cursor-pointer"
                    title="New Direct Message"
                  >
                    <Plus className="w-3.5 h-3.5" />
                  </button>
                </div>

                <div className="space-y-0.5">
                  {directConvos.length === 0 ? (
                    <div className="px-2 py-3 text-center text-xs text-neutral-400 italic">
                      No direct messages yet
                    </div>
                  ) : (
                    directConvos.map((convo) => {
                      const isSelected =
                        activeChat?.type === 'dm' &&
                        activeChat.conversation.id === convo.id;
                      return (
                        <button
                          key={convo.id}
                          onClick={() => {
                            setActiveChat({ type: 'dm', conversation: convo });
                            setShowMobileChat(true);
                          }}
                          className={`w-full flex items-center justify-between px-2.5 py-2 rounded-lg text-xs transition-colors text-left ${
                            isSelected
                              ? 'bg-neutral-900 text-white dark:bg-white dark:text-neutral-950 shadow-xs'
                              : 'text-neutral-700 hover:bg-neutral-200/60 dark:text-neutral-300 dark:hover:bg-[#202020]'
                          }`}
                        >
                          <div className="flex items-center gap-2 min-w-0">
                            <div className="w-6 h-6 rounded-full bg-neutral-200 dark:bg-[#262626] flex items-center justify-center text-[10px] font-bold shrink-0">
                              {convo.otherUserName?.charAt(0).toUpperCase() || 'U'}
                            </div>
                            <div className="min-w-0">
                              <p className="font-medium truncate leading-tight">
                                {convo.otherUserName}
                              </p>
                              {convo.lastMessage && (
                                <p
                                  className={`text-[11px] truncate mt-0.5 ${
                                    isSelected
                                      ? 'text-neutral-300 dark:text-neutral-600'
                                      : 'text-neutral-400'
                                  }`}
                                >
                                  {convo.lastMessage}
                                </p>
                              )}
                            </div>
                          </div>
                          {convo.unreadCount > 0 && !isSelected && (
                            <span className="shrink-0 bg-blue-600 text-white text-[10px] font-bold px-1.5 py-0.5 rounded-full">
                              {convo.unreadCount}
                            </span>
                          )}
                        </button>
                      );
                    })
                  )}
                </div>
              </div>
            </>
          )}
        </div>
      </div>

      {/* ------------------------------------------------------------- */}
      {/* MAIN CHAT PANE                                                */}
      {/* ------------------------------------------------------------- */}
      <div
        className={`flex-1 flex flex-col bg-white dark:bg-[#101010] ${
          !showMobileChat ? 'hidden md:flex' : 'flex'
        }`}
      >
        {activeChat ? (
          <>
            {/* Chat Header */}
            <div className="h-14 border-b border-neutral-200 dark:border-[#262626] px-4 flex items-center justify-between shrink-0 bg-white/50 dark:bg-[#101010]/50 backdrop-blur-xs">
              <div className="flex items-center gap-3 min-w-0">
                <button
                  onClick={() => setShowMobileChat(false)}
                  className="md:hidden p-1.5 -ml-1 text-neutral-500 hover:text-neutral-900 dark:hover:text-white"
                >
                  <ChevronLeft className="w-5 h-5" />
                </button>

                {activeChat.type === 'channel' ? (
                  <div className="flex items-center gap-2 min-w-0">
                    <div className="p-1.5 rounded-lg bg-neutral-100 dark:bg-[#202020] text-neutral-600 dark:text-neutral-300 shrink-0">
                      {activeChat.channel.type === 'PROJECT' ? (
                        <FolderKanban className="w-4 h-4" />
                      ) : (
                        <Hash className="w-4 h-4" />
                      )}
                    </div>
                    <div className="min-w-0">
                      <div className="flex items-center gap-2">
                        <h3 className="text-sm font-semibold text-neutral-900 dark:text-neutral-100 truncate">
                          {activeChat.channel.name}
                        </h3>
                        <Badge variant="default" size="sm">
                          {activeChat.channel.type}
                        </Badge>
                      </div>
                      {activeChat.channel.topic && (
                        <p className="text-[11px] text-neutral-400 truncate">
                          {activeChat.channel.topic}
                        </p>
                      )}
                    </div>
                  </div>
                ) : (
                  <div className="flex items-center gap-2.5 min-w-0">
                    <div className="w-8 h-8 rounded-full bg-neutral-200 dark:bg-[#262626] flex items-center justify-center text-xs font-bold shrink-0">
                      {activeChat.conversation.otherUserName?.charAt(0).toUpperCase() || 'U'}
                    </div>
                    <div className="min-w-0">
                      <div className="flex items-center gap-2">
                        <h3 className="text-sm font-semibold text-neutral-900 dark:text-neutral-100 truncate">
                          {activeChat.conversation.otherUserName}
                        </h3>
                        <Badge variant="default" size="sm">
                          {activeChat.conversation.otherUserRole}
                        </Badge>
                      </div>
                      <p className="text-[11px] text-neutral-400 truncate">
                        {activeChat.conversation.otherUserEmail}
                      </p>
                    </div>
                  </div>
                )}
              </div>

              {/* Live Connection Status Indicator */}
              <div className="flex items-center gap-1.5 px-2.5 py-1 rounded-full text-[11px] font-medium border border-neutral-200 dark:border-[#262626] bg-neutral-50/80 dark:bg-[#161616] shrink-0">
                <span
                  className={`w-2 h-2 rounded-full ${
                    isConnected
                      ? 'bg-emerald-500 shadow-[0_0_6px_rgba(16,185,129,0.5)]'
                      : 'bg-amber-500 animate-pulse'
                  }`}
                />
                <span className="text-neutral-600 dark:text-neutral-400">
                  {isConnected ? 'Live' : 'Connecting...'}
                </span>
              </div>
            </div>

            {/* Messages Feed */}
            <div className="flex-1 overflow-y-auto p-4 space-y-4">
              {isLoadingMessages ? (
                <div className="space-y-4 p-4">
                  <LoadingSkeleton variant="text" rows={5} />
                </div>
              ) : (
                <>
                  {activeChat.type === 'channel' ? (
                    channelMessages.length === 0 ? (
                      <div className="h-full flex flex-col items-center justify-center text-center p-8 text-neutral-400">
                        <MessageSquare className="w-10 h-10 mb-2 opacity-40" />
                        <p className="text-sm font-medium">Welcome to #{activeChat.channel.name}!</p>
                        <p className="text-xs mt-1">This is the start of the #{activeChat.channel.name} channel.</p>
                      </div>
                    ) : (
                      channelMessages.map((msg) => {
                        const isSelf = msg.senderId === user?.id;
                        return (
                          <div
                            key={msg.id}
                            className={`flex gap-3 text-xs group ${isSelf ? 'justify-end' : 'justify-start'}`}
                          >
                            {!isSelf && (
                              <div className="w-7 h-7 rounded-full bg-neutral-200 dark:bg-[#262626] flex items-center justify-center font-bold text-[11px] shrink-0 text-neutral-700 dark:text-neutral-200">
                                {msg.senderName?.charAt(0).toUpperCase() || 'U'}
                              </div>
                            )}
                            <div
                              className={`max-w-[75%] rounded-2xl px-3.5 py-2.5 ${
                                isSelf
                                  ? 'bg-neutral-900 text-white dark:bg-neutral-100 dark:text-neutral-950 rounded-br-xs'
                                  : 'bg-neutral-100 text-neutral-900 dark:bg-[#202020] dark:text-neutral-100 rounded-bl-xs'
                              }`}
                            >
                              {!isSelf && (
                                <div className="flex items-center gap-2 mb-1">
                                  <span className="font-semibold text-[11px] text-neutral-700 dark:text-neutral-300">
                                    {msg.senderName}
                                  </span>
                                  <span className="text-[10px] text-neutral-400">
                                    {formatTime(msg.createdAt)}
                                  </span>
                                </div>
                              )}
                              <div className="leading-relaxed whitespace-pre-wrap break-words">
                                {highlightMentions(msg.content)}
                              </div>
                              {isSelf && (
                                <div className="text-[10px] text-neutral-300 dark:text-neutral-500 text-right mt-1">
                                  {formatTime(msg.createdAt)}
                                </div>
                              )}
                            </div>
                          </div>
                        );
                      })
                    )
                  ) : directMessages.length === 0 ? (
                    <div className="h-full flex flex-col items-center justify-center text-center p-8 text-neutral-400">
                      <User className="w-10 h-10 mb-2 opacity-40" />
                      <p className="text-sm font-medium">Direct Conversation</p>
                      <p className="text-xs mt-1">
                        Send a message to start chatting with {activeChat.conversation.otherUserName}.
                      </p>
                    </div>
                  ) : (
                    directMessages.map((msg) => {
                      const isSelf = msg.senderId === user?.id;
                      return (
                        <div
                          key={msg.id}
                          className={`flex gap-3 text-xs ${isSelf ? 'justify-end' : 'justify-start'}`}
                        >
                          {!isSelf && (
                            <div className="w-7 h-7 rounded-full bg-neutral-200 dark:bg-[#262626] flex items-center justify-center font-bold text-[11px] shrink-0 text-neutral-700 dark:text-neutral-200">
                              {msg.senderName?.charAt(0).toUpperCase() || 'U'}
                            </div>
                          )}
                          <div
                            className={`max-w-[75%] rounded-2xl px-3.5 py-2.5 ${
                              isSelf
                                ? 'bg-neutral-900 text-white dark:bg-neutral-100 dark:text-neutral-950 rounded-br-xs'
                                : 'bg-neutral-100 text-neutral-900 dark:bg-[#202020] dark:text-neutral-100 rounded-bl-xs'
                            }`}
                          >
                            {!isSelf && (
                              <div className="flex items-center gap-2 mb-1">
                                <span className="font-semibold text-[11px] text-neutral-700 dark:text-neutral-300">
                                  {msg.senderName}
                                </span>
                                <span className="text-[10px] text-neutral-400">
                                  {formatTime(msg.createdAt)}
                                </span>
                              </div>
                            )}
                            <div className="leading-relaxed whitespace-pre-wrap break-words">
                              {highlightMentions(msg.content)}
                            </div>
                            {isSelf && (
                              <div className="text-[10px] text-neutral-300 dark:text-neutral-500 text-right mt-1">
                                {formatTime(msg.createdAt)}
                              </div>
                            )}
                          </div>
                        </div>
                      );
                    })
                  )}
                  <div ref={messagesEndRef} />
                </>
              )}
            </div>

            {/* Message Input Box */}
            <div className="p-3 border-t border-neutral-200 dark:border-[#262626] bg-white dark:bg-[#101010]">
              <form
                onSubmit={handleSendMessage}
                className="flex items-end gap-2 bg-neutral-50 dark:bg-[#181818] border border-neutral-200 dark:border-[#262626] rounded-xl p-2 focus-within:border-neutral-400 dark:focus-within:border-neutral-600 transition-colors"
              >
                <textarea
                  value={messageInput}
                  onChange={(e) => setMessageInput(e.target.value)}
                  onKeyDown={(e) => {
                    if (e.key === 'Enter' && !e.shiftKey) {
                      e.preventDefault();
                      handleSendMessage();
                    }
                  }}
                  placeholder={
                    activeChat.type === 'channel'
                      ? `Message #${activeChat.channel.name} (use @ to mention teammates)`
                      : `Message ${activeChat.conversation.otherUserName}...`
                  }
                  rows={1}
                  className="flex-1 bg-transparent resize-none border-0 text-xs text-neutral-900 dark:text-neutral-100 placeholder-neutral-400 focus:outline-hidden max-h-32 min-h-6 py-1 px-1.5"
                />
                <Button
                  type="submit"
                  size="sm"
                  disabled={!messageInput.trim() || isSending}
                  className="h-8 w-8 !p-0 shrink-0 rounded-lg"
                  aria-label="Send Message"
                >
                  <Send className="w-3.5 h-3.5" />
                </Button>
              </form>
              <div className="flex items-center justify-between px-2 pt-1.5 text-[10px] text-neutral-400">
                <span>Tip: Press Enter to send, Shift+Enter for new line</span>
                <span className="flex items-center gap-1">
                  <AtSign className="w-2.5 h-2.5" /> Mentions notify active members
                </span>
              </div>
            </div>
          </>
        ) : (
          <div className="flex-1 flex flex-col items-center justify-center text-center p-8 text-neutral-400">
            <MessageSquare className="w-12 h-12 mb-3 opacity-30" />
            <h3 className="text-base font-semibold text-neutral-700 dark:text-neutral-300">
              Select a channel or message
            </h3>
            <p className="text-xs text-neutral-500 mt-1 max-w-sm">
              Choose from the sidebar on the left to start collaborating with your team in real time.
            </p>
          </div>
        )}
      </div>

      {/* ------------------------------------------------------------- */}
      {/* MODAL: Create New Channel                                     */}
      {/* ------------------------------------------------------------- */}
      <Modal
        isOpen={isNewChannelOpen}
        onClose={() => setIsNewChannelOpen(false)}
        title="Create Channel"
        description="Channels are where your team discusses projects and topics."
      >
        <form onSubmit={handleCreateChannel} className="space-y-4">
          {channelError && (
            <div className="p-2.5 rounded-lg bg-rose-50 dark:bg-rose-950/40 border border-rose-200 dark:border-rose-900/50 text-rose-600 dark:text-rose-400 text-xs">
              {channelError}
            </div>
          )}

          <div>
            <label className="block text-xs font-semibold text-neutral-700 dark:text-neutral-300 mb-1">
              Channel Name
            </label>
            <div className="relative">
              <span className="absolute left-3 top-2.5 text-neutral-400 text-xs">#</span>
              <Input
                value={newChannelName}
                onChange={(e) => setNewChannelName(e.target.value)}
                placeholder="e.g. product-launch"
                className="pl-7 text-xs"
                required
              />
            </div>
            <p className="text-[11px] text-neutral-400 mt-1">
              Names must be lowercase with no spaces (e.g. announcements).
            </p>
          </div>

          <div>
            <label className="block text-xs font-semibold text-neutral-700 dark:text-neutral-300 mb-1">
              Topic / Purpose (Optional)
            </label>
            <Input
              value={newChannelTopic}
              onChange={(e) => setNewChannelTopic(e.target.value)}
              placeholder="What is this channel about?"
              className="text-xs"
            />
          </div>

          <div className="flex justify-end gap-2 pt-2">
            <Button
              type="button"
              variant="outline"
              size="sm"
              onClick={() => setIsNewChannelOpen(false)}
            >
              Cancel
            </Button>
            <Button type="submit" size="sm">
              Create Channel
            </Button>
          </div>
        </form>
      </Modal>

      {/* ------------------------------------------------------------- */}
      {/* MODAL: Start Direct Message                                   */}
      {/* ------------------------------------------------------------- */}
      <Modal
        isOpen={isNewDmOpen}
        onClose={() => setIsNewDmOpen(false)}
        title="New Message"
        description="Select a teammate to start a direct 1-to-1 conversation."
      >
        <div className="space-y-4">
          <Input
            value={dmSearchQuery}
            onChange={(e) => setDmSearchQuery(e.target.value)}
            placeholder="Search teammates by name or email..."
            className="text-xs"
          />

          <div className="max-h-60 overflow-y-auto space-y-1 divide-y divide-neutral-100 dark:divide-[#202020]">
            {workspaceUsers
              .filter((u) => {
                const name = getUserDisplayName(u).toLowerCase();
                const q = dmSearchQuery.toLowerCase();
                return name.includes(q) || u.email.toLowerCase().includes(q);
              })
              .map((u) => {
                const displayName = getUserDisplayName(u);
                return (
                  <button
                    key={u.id}
                    type="button"
                    onClick={() => handleStartDm(u.id)}
                    className="w-full flex items-center justify-between p-2.5 rounded-lg hover:bg-neutral-100 dark:hover:bg-[#202020] text-left transition-colors cursor-pointer"
                  >
                    <div className="flex items-center gap-2.5 min-w-0">
                      <div className="w-8 h-8 rounded-full bg-neutral-200 dark:bg-[#262626] flex items-center justify-center text-xs font-bold shrink-0">
                        {displayName.charAt(0).toUpperCase()}
                      </div>
                      <div className="min-w-0">
                        <p className="text-xs font-semibold text-neutral-900 dark:text-neutral-100 truncate">
                          {displayName}
                        </p>
                        <p className="text-[11px] text-neutral-400 truncate">{u.email}</p>
                      </div>
                    </div>
                    <Badge variant="default" size="sm">
                      {u.role}
                    </Badge>
                  </button>
                );
              })}
            {workspaceUsers.length === 0 && (
              <div className="p-4 text-center text-xs text-neutral-400">
                <Users className="w-8 h-8 mx-auto mb-2 opacity-40" />
                No other teammates found in this workspace.
              </div>
            )}
          </div>
        </div>
      </Modal>
    </div>
  );
};
