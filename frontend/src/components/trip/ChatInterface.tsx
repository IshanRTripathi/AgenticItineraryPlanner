/**
 * Chat Interface Component
 * Real-time chat with AI for itinerary modifications via WebSocket
 * Task 11: Mobile-optimized chat with sticky input and proper message bubbles
 */

import { useState, useRef, useEffect } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Badge } from '@/components/ui/badge';
import { Send, Bot, User, Loader2, Wifi, WifiOff, Paperclip } from 'lucide-react';
import { apiClient } from '@/services/apiClient';
import { useStompWebSocket } from '@/hooks/useStompWebSocket';
import { useMediaQuery } from '@/hooks/useMediaQuery';

interface Message {
  id: string;
  role: 'user' | 'assistant';
  content: string;
  timestamp: Date;
}

interface ChatInterfaceProps {
  itineraryId: string;
}

export function ChatInterface({ itineraryId }: ChatInterfaceProps) {
  const isMobile = useMediaQuery('(max-width: 768px)');
  const [messages, setMessages] = useState<Message[]>([]);
  const [input, setInput] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLInputElement>(null);

  // WebSocket connection for real-time chat
  const { isConnected, sendMessage: sendWsMessage } = useStompWebSocket(
    itineraryId,
    {
      onMessage: (data: any) => {
        if (data.type === 'chat_response') {
          const assistantMessage: Message = {
            id: Date.now().toString(),
            role: 'assistant',
            content: data.message,
            timestamp: new Date(data.timestamp),
          };
          setMessages((prev) => [...prev, assistantMessage]);
          setIsLoading(false);
        }
      },
    }
  );

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  useEffect(() => {
    scrollToBottom();
  }, [messages]);

  // Load chat history on mount
  useEffect(() => {
    const loadHistory = async () => {
      try {
        const response = await apiClient.get(`/itineraries/${itineraryId}/chat/history`);
        if (response.data?.messages) {
          setMessages(response.data.messages.map((m: any) => ({
            ...m,
            timestamp: new Date(m.timestamp),
          })));
        }
      } catch (error) {
        console.error('Failed to load chat history:', error);
      }
    };
    loadHistory();
  }, [itineraryId]);

  const handleSend = async () => {
    if (!input.trim() || isLoading) return;

    const userMessage: Message = {
      id: Date.now().toString(),
      role: 'user',
      content: input,
      timestamp: new Date(),
    };

    setMessages((prev) => [...prev, userMessage]);
    const messageContent = input;
    setInput('');
    setIsLoading(true);

    try {
      // Send via WebSocket if connected, otherwise use HTTP
      if (isConnected) {
        sendWsMessage(`/app/chat/${itineraryId}`, {
          message: messageContent,
          timestamp: new Date().toISOString(),
        });
      } else {
        // Fallback to HTTP
        const response = await apiClient.post(`/itineraries/${itineraryId}/chat`, {
          message: messageContent,
        });

        const assistantMessage: Message = {
          id: (Date.now() + 1).toString(),
          role: 'assistant',
          content: response.data.response || 'I can help you modify your itinerary.',
          timestamp: new Date(),
        };

        setMessages((prev) => [...prev, assistantMessage]);
        setIsLoading(false);
      }
    } catch (error) {
      const errorMessage: Message = {
        id: (Date.now() + 1).toString(),
        role: 'assistant',
        content: 'Sorry, I encountered an error. Please try again.',
        timestamp: new Date(),
      };
      setMessages((prev) => [...prev, errorMessage]);
      setIsLoading(false);
    }
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  };

  return (
    <Card className="h-[500px] sm:h-[600px] flex flex-col">
      <CardHeader className="border-b p-3 sm:p-6">
        <CardTitle className="flex items-center gap-2 text-base sm:text-lg">
          <Bot className="w-4 h-4 sm:w-5 sm:h-5 text-primary" />
          <span className="hidden sm:inline">AI Assistant</span>
          <span className="sm:hidden">AI Chat</span>
          <Badge variant="secondary" className="ml-1 sm:ml-2 text-xs">Beta</Badge>
          <div className="ml-auto flex items-center gap-2">
            {isConnected ? (
              <Badge variant="outline" className="gap-1 text-xs">
                <Wifi className="w-3 h-3" />
                <span className="hidden sm:inline">Live</span>
              </Badge>
            ) : (
              <Badge variant="outline" className="gap-1 opacity-50 text-xs">
                <WifiOff className="w-3 h-3" />
                <span className="hidden sm:inline">Offline</span>
              </Badge>
            )}
          </div>
        </CardTitle>
      </CardHeader>

      <CardContent className="flex-1 overflow-y-auto p-3 sm:p-4 space-y-3 sm:space-y-4">
        {messages.length === 0 && (
          <div className="text-center text-muted-foreground py-6 sm:py-8 px-4">
            <Bot className="w-10 h-10 sm:w-12 sm:h-12 mx-auto mb-3 text-muted-foreground/50" />
            <p className="text-sm sm:text-base">Ask me to modify your itinerary!</p>
            <p className="text-xs sm:text-sm mt-2">
              Try: "Add a restaurant for dinner"
            </p>
          </div>
        )}

        {messages.map((message) => (
          <div
            key={message.id}
            className={`flex gap-2 sm:gap-3 ${
              message.role === 'user' ? 'justify-end' : 'justify-start'
            }`}
          >
            {message.role === 'assistant' && (
              <div className="w-7 h-7 sm:w-8 sm:h-8 rounded-full bg-primary/10 flex items-center justify-center shrink-0">
                <Bot className="w-3.5 h-3.5 sm:w-4 sm:h-4 text-primary" />
              </div>
            )}

            <div
              className={`max-w-[85%] sm:max-w-[75%] rounded-lg px-3 py-2 sm:px-4 sm:py-2.5 ${
                message.role === 'user'
                  ? 'bg-primary text-primary-foreground'
                  : 'bg-muted'
              }`}
            >
              <p className="text-sm leading-relaxed break-words">{message.content}</p>
              <p className="text-xs opacity-70 mt-1">
                {message.timestamp.toLocaleTimeString([], {
                  hour: '2-digit',
                  minute: '2-digit',
                })}
              </p>
            </div>

            {message.role === 'user' && (
              <div className="w-7 h-7 sm:w-8 sm:h-8 rounded-full bg-primary flex items-center justify-center shrink-0">
                <User className="w-3.5 h-3.5 sm:w-4 sm:h-4 text-primary-foreground" />
              </div>
            )}
          </div>
        ))}

        {isLoading && (
          <div className="flex gap-2 sm:gap-3 justify-start">
            <div className="w-7 h-7 sm:w-8 sm:h-8 rounded-full bg-primary/10 flex items-center justify-center shrink-0">
              <Bot className="w-3.5 h-3.5 sm:w-4 sm:h-4 text-primary" />
            </div>
            <div className="bg-muted rounded-lg px-3 py-2 sm:px-4 sm:py-2.5">
              <Loader2 className="w-4 h-4 animate-spin" />
            </div>
          </div>
        )}

        <div ref={messagesEndRef} />
      </CardContent>

      {/* Sticky input area with safe area padding */}
      <div className="p-3 sm:p-4 border-t bg-background sticky bottom-0 safe-bottom">
        <div className="flex gap-2">
          <Input
            ref={inputRef}
            value={input}
            onChange={(e) => setInput(e.target.value)}
            onKeyDown={handleKeyDown}
            placeholder={isMobile ? "Ask me..." : "Ask me to modify your trip..."}
            disabled={isLoading}
            className="flex-1 min-h-[44px] text-base"
          />
          <Button
            onClick={handleSend}
            disabled={!input.trim() || isLoading}
            size="icon"
            className="min-w-[44px] min-h-[44px] touch-manipulation active:scale-95 transition-transform"
          >
            {isLoading ? (
              <Loader2 className="w-4 h-4 sm:w-5 sm:h-5 animate-spin" />
            ) : (
              <Send className="w-4 h-4 sm:w-5 sm:h-5" />
            )}
          </Button>
        </div>
      </div>
    </Card>
  );
}
