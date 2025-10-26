/**
 * User Profile Page
 * View and edit user profile information
 */

import { useState } from 'react';
import { Header } from '@/components/layout/Header';
import { Footer } from '@/components/layout/Footer';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Avatar } from '@/components/ui/avatar';
import { Tabs, TabsList, TabsTrigger, TabsContent } from '@/components/ui/tabs';
import { User, Mail, Phone, MapPin, Calendar, Camera, Lock } from 'lucide-react';

// Mock user data
const MOCK_USER = {
  name: 'John Doe',
  email: 'john.doe@example.com',
  phone: '+1 (555) 123-4567',
  location: 'New York, USA',
  joinDate: 'January 2024',
  avatar: 'https://api.dicebear.com/7.x/avataaars/svg?seed=John',
};

export function ProfilePage() {
  const [isEditing, setIsEditing] = useState(false);
  const [formData, setFormData] = useState(MOCK_USER);

  const handleSave = () => {
    // TODO: Implement save logic
    console.log('Save profile:', formData);
    setIsEditing(false);
  };

  return (
    <div className="min-h-screen flex flex-col">
      <Header />
      
      <main className="flex-1 bg-muted/30 py-12">
        <div className="container max-w-4xl">
          {/* Header */}
          <div className="mb-8">
            <h1 className="text-4xl font-bold text-foreground mb-2">
              My Profile
            </h1>
            <p className="text-muted-foreground">
              Manage your account settings and preferences
            </p>
          </div>

          <Tabs value="profile" onValueChange={() => {}}>
            <TabsList className="mb-6">
              <TabsTrigger value="profile">Profile</TabsTrigger>
              <TabsTrigger value="security">Security</TabsTrigger>
              <TabsTrigger value="preferences">Preferences</TabsTrigger>
            </TabsList>

            {/* Profile Tab */}
            <TabsContent value="profile">
              <Card>
                <CardHeader>
                  <div className="flex items-start justify-between">
                    <div>
                      <CardTitle>Profile Information</CardTitle>
                      <CardDescription>
                        Update your personal details and contact information
                      </CardDescription>
                    </div>
                    {!isEditing && (
                      <Button onClick={() => setIsEditing(true)}>
                        Edit Profile
                      </Button>
                    )}
                  </div>
                </CardHeader>
                <CardContent>
                  {/* Avatar */}
                  <div className="flex items-center gap-6 mb-8">
                    <Avatar className="w-24 h-24">
                      <img src={MOCK_USER.avatar} alt={MOCK_USER.name} />
                    </Avatar>
                    {isEditing && (
                      <Button variant="outline" size="sm">
                        <Camera className="w-4 h-4 mr-2" />
                        Change Photo
                      </Button>
                    )}
                  </div>

                  {/* Form */}
                  <div className="space-y-6">
                    {/* Name */}
                    <div>
                      <Label htmlFor="name">Full Name</Label>
                      <div className="relative mt-2">
                        <User className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-muted-foreground" />
                        <Input
                          id="name"
                          value={formData.name}
                          onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                          className="pl-10"
                          disabled={!isEditing}
                        />
                      </div>
                    </div>

                    {/* Email */}
                    <div>
                      <Label htmlFor="email">Email</Label>
                      <div className="relative mt-2">
                        <Mail className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-muted-foreground" />
                        <Input
                          id="email"
                          type="email"
                          value={formData.email}
                          onChange={(e) => setFormData({ ...formData, email: e.target.value })}
                          className="pl-10"
                          disabled={!isEditing}
                        />
                      </div>
                    </div>

                    {/* Phone */}
                    <div>
                      <Label htmlFor="phone">Phone Number</Label>
                      <div className="relative mt-2">
                        <Phone className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-muted-foreground" />
                        <Input
                          id="phone"
                          type="tel"
                          value={formData.phone}
                          onChange={(e) => setFormData({ ...formData, phone: e.target.value })}
                          className="pl-10"
                          disabled={!isEditing}
                        />
                      </div>
                    </div>

                    {/* Location */}
                    <div>
                      <Label htmlFor="location">Location</Label>
                      <div className="relative mt-2">
                        <MapPin className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-muted-foreground" />
                        <Input
                          id="location"
                          value={formData.location}
                          onChange={(e) => setFormData({ ...formData, location: e.target.value })}
                          className="pl-10"
                          disabled={!isEditing}
                        />
                      </div>
                    </div>

                    {/* Member Since */}
                    <div>
                      <Label>Member Since</Label>
                      <div className="flex items-center gap-2 mt-2 text-muted-foreground">
                        <Calendar className="w-5 h-5" />
                        <span>{formData.joinDate}</span>
                      </div>
                    </div>

                    {/* Actions */}
                    {isEditing && (
                      <div className="flex gap-3 pt-4">
                        <Button onClick={handleSave}>
                          Save Changes
                        </Button>
                        <Button
                          variant="outline"
                          onClick={() => {
                            setFormData(MOCK_USER);
                            setIsEditing(false);
                          }}
                        >
                          Cancel
                        </Button>
                      </div>
                    )}
                  </div>
                </CardContent>
              </Card>
            </TabsContent>

            {/* Security Tab */}
            <TabsContent value="security">
              <Card>
                <CardHeader>
                  <CardTitle>Security Settings</CardTitle>
                  <CardDescription>
                    Manage your password and security preferences
                  </CardDescription>
                </CardHeader>
                <CardContent>
                  <div className="space-y-6">
                    {/* Change Password */}
                    <div>
                      <h3 className="font-semibold mb-4 flex items-center gap-2">
                        <Lock className="w-5 h-5" />
                        Change Password
                      </h3>
                      <div className="space-y-4">
                        <div>
                          <Label htmlFor="currentPassword">Current Password</Label>
                          <Input
                            id="currentPassword"
                            type="password"
                            placeholder="••••••••"
                            className="mt-2"
                          />
                        </div>
                        <div>
                          <Label htmlFor="newPassword">New Password</Label>
                          <Input
                            id="newPassword"
                            type="password"
                            placeholder="••••••••"
                            className="mt-2"
                          />
                        </div>
                        <div>
                          <Label htmlFor="confirmNewPassword">Confirm New Password</Label>
                          <Input
                            id="confirmNewPassword"
                            type="password"
                            placeholder="••••••••"
                            className="mt-2"
                          />
                        </div>
                        <Button>Update Password</Button>
                      </div>
                    </div>

                    {/* Two-Factor Authentication */}
                    <div className="pt-6 border-t">
                      <h3 className="font-semibold mb-2">Two-Factor Authentication</h3>
                      <p className="text-sm text-muted-foreground mb-4">
                        Add an extra layer of security to your account
                      </p>
                      <Button variant="outline">Enable 2FA</Button>
                    </div>
                  </div>
                </CardContent>
              </Card>
            </TabsContent>

            {/* Preferences Tab */}
            <TabsContent value="preferences">
              <Card>
                <CardHeader>
                  <CardTitle>Preferences</CardTitle>
                  <CardDescription>
                    Customize your experience
                  </CardDescription>
                </CardHeader>
                <CardContent>
                  <div className="space-y-6">
                    {/* Email Notifications */}
                    <div>
                      <h3 className="font-semibold mb-4">Email Notifications</h3>
                      <div className="space-y-3">
                        <label className="flex items-center gap-3">
                          <input type="checkbox" defaultChecked />
                          <div>
                            <div className="font-medium">Trip Updates</div>
                            <div className="text-sm text-muted-foreground">
                              Receive updates about your trips
                            </div>
                          </div>
                        </label>
                        <label className="flex items-center gap-3">
                          <input type="checkbox" defaultChecked />
                          <div>
                            <div className="font-medium">Booking Confirmations</div>
                            <div className="text-sm text-muted-foreground">
                              Get notified when bookings are confirmed
                            </div>
                          </div>
                        </label>
                        <label className="flex items-center gap-3">
                          <input type="checkbox" />
                          <div>
                            <div className="font-medium">Promotional Emails</div>
                            <div className="text-sm text-muted-foreground">
                              Receive special offers and deals
                            </div>
                          </div>
                        </label>
                      </div>
                    </div>

                    {/* Currency */}
                    <div className="pt-6 border-t">
                      <Label htmlFor="currency">Preferred Currency</Label>
                      <select
                        id="currency"
                        className="mt-2 w-full h-12 px-4 rounded-md border border-input bg-background"
                      >
                        <option>USD - US Dollar</option>
                        <option>EUR - Euro</option>
                        <option>GBP - British Pound</option>
                        <option>JPY - Japanese Yen</option>
                      </select>
                    </div>

                    <Button>Save Preferences</Button>
                  </div>
                </CardContent>
              </Card>
            </TabsContent>
          </Tabs>
        </div>
      </main>

      <Footer />
    </div>
  );
}
