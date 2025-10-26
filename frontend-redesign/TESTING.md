# Testing Guide

## 🧪 Testing Strategy

This guide covers manual testing procedures for the EasyTrip frontend application.

---

## ✅ Pre-Deployment Testing Checklist

### Build & Performance
- [ ] Production build completes without errors: `npm run build`
- [ ] Bundle size is acceptable (~280KB, ~90KB gzipped)
- [ ] No console errors in production build
- [ ] All routes load correctly
- [ ] Page load time < 3 seconds on 3G

### Functionality Testing

#### Homepage
- [ ] Hero section displays correctly
- [ ] Search widget tabs switch properly
- [ ] All 5 search forms render (Flights, Hotels, Holidays, Trains, Bus)
- [ ] Form inputs accept data
- [ ] Trending destinations grid displays
- [ ] Popular routes carousel scrolls horizontally
- [ ] Travel blogs grid displays
- [ ] "Let AI Plan My Itinerary" button navigates to wizard

#### AI Trip Wizard
- [ ] Wizard opens at Step 1
- [ ] Progress indicator shows current step
- [ ] Step 1: Destination input works, popular destinations clickable
- [ ] Step 2: Date pickers work, traveler counters increment/decrement
- [ ] Step 3: Budget/pace/interests selection works
- [ ] Step 4: Review shows all entered data
- [ ] Back button navigates to previous step
- [ ] Next button advances to next step
- [ ] Create Itinerary button navigates to progress page

#### AI Agent Progress
- [ ] Progress screen displays with animations
- [ ] Progress bar animates from 0-100%
- [ ] Steps update in real-time
- [ ] Motivational messages rotate
- [ ] Particle animations render smoothly

#### Dashboard
- [ ] Trip list displays
- [ ] Filter tabs work (All, Upcoming, Completed)
- [ ] Trip cards show correct information
- [ ] "View Details" button navigates to trip detail
- [ ] "Plan New Trip" button navigates to wizard

#### Trip Detail
- [ ] Hero image displays
- [ ] Trip information shows correctly
- [ ] Day-by-day itinerary renders
- [ ] Activity timeline displays with icons
- [ ] "Book Now" buttons open booking modal
- [ ] Tabs switch (Itinerary, Bookings, Documents)
- [ ] Bookings tab shows booking cards

#### Booking System
- [ ] Booking modal opens
- [ ] Iframe placeholder displays
- [ ] "Simulate Success" shows success state
- [ ] "Simulate Failure" shows error state
- [ ] Success state displays confirmation details
- [ ] Modal closes properly

#### Search Results
- [ ] Results display in grid
- [ ] Filters sidebar shows
- [ ] Result cards show all information
- [ ] "Book Now" buttons open booking modal
- [ ] Pricing displays correctly

#### Authentication
- [ ] Login page displays
- [ ] Email/password inputs work
- [ ] Password visibility toggle works
- [ ] Social login buttons present
- [ ] "Sign up" link navigates to signup
- [ ] Signup page displays
- [ ] All form fields work
- [ ] Terms checkbox required
- [ ] Form validation works

#### Profile
- [ ] Profile page displays
- [ ] Tabs switch (Profile, Security, Preferences)
- [ ] Edit mode enables form fields
- [ ] Save/Cancel buttons work
- [ ] Avatar displays
- [ ] Password change form works
- [ ] Preferences checkboxes work

### Responsive Design

#### Mobile (< 768px)
- [ ] Mobile menu icon appears
- [ ] Mobile menu slides out when clicked
- [ ] All menu items work
- [ ] Search widget stacks vertically
- [ ] Forms are full-width
- [ ] Buttons are full-width
- [ ] Cards stack in single column
- [ ] Touch targets are ≥48px
- [ ] Text is readable (≥16px)

#### Tablet (768px - 1023px)
- [ ] Layout adjusts appropriately
- [ ] Grids show 2-3 columns
- [ ] Navigation is visible
- [ ] Forms use appropriate width

#### Desktop (≥1024px)
- [ ] Full navigation visible
- [ ] Grids show 3-4 columns
- [ ] Optimal spacing and layout
- [ ] Hover effects work

### Accessibility

#### Keyboard Navigation
- [ ] Tab key navigates through interactive elements
- [ ] Enter key activates buttons/links
- [ ] Escape key closes modals
- [ ] Focus indicators visible
- [ ] Skip to content link works (if implemented)

#### Screen Reader
- [ ] Images have alt text
- [ ] Buttons have descriptive labels
- [ ] Form inputs have associated labels
- [ ] Error messages are announced
- [ ] Page title updates on route change

#### Visual
- [ ] Text contrast meets WCAG AA (≥4.5:1)
- [ ] Large text contrast meets WCAG AA (≥3:1)
- [ ] Focus indicators are visible
- [ ] No content relies solely on color
- [ ] Text can be resized to 200%

### Browser Compatibility

#### Chrome/Edge (Chromium)
- [ ] All features work
- [ ] Animations smooth
- [ ] No console errors

#### Firefox
- [ ] All features work
- [ ] Animations smooth
- [ ] No console errors

#### Safari
- [ ] All features work
- [ ] Animations smooth
- [ ] No console errors
- [ ] iOS Safari tested

### Error Handling

- [ ] Error boundary catches errors
- [ ] Error page displays with helpful message
- [ ] "Refresh Page" button works
- [ ] "Go Home" button works
- [ ] Network errors show appropriate message
- [ ] Form validation errors display
- [ ] Loading states show during async operations

### Performance

- [ ] Images load progressively
- [ ] Animations run at 60fps
- [ ] No layout shifts during load
- [ ] Smooth scrolling
- [ ] Fast route transitions
- [ ] No memory leaks (check DevTools)

---

## 🔍 Testing Procedures

### Manual Testing Flow

**1. Homepage Flow**
```
1. Open homepage
2. Verify hero section loads
3. Click each search tab
4. Fill out flight search form
5. Click trending destination
6. Scroll popular routes
7. Click blog card
8. Click "Let AI Plan" button
```

**2. Trip Creation Flow**
```
1. Click "Let AI Plan My Itinerary"
2. Enter destination
3. Click Next
4. Select dates and travelers
5. Click Next
6. Choose preferences
7. Click Next
8. Review details
9. Click "Create Itinerary"
10. Verify progress screen
11. Wait for completion
```

**3. Dashboard Flow**
```
1. Navigate to /dashboard
2. Verify trips display
3. Click filter tabs
4. Click "View Details" on a trip
5. Verify trip detail page
6. Click "Book Now" on activity
7. Test booking modal
8. Navigate back to dashboard
```

**4. Authentication Flow**
```
1. Click "Sign In"
2. Try invalid credentials
3. Verify error message
4. Click "Sign up"
5. Fill registration form
6. Submit form
7. Verify redirect to dashboard
```

### Device Testing

**Mobile Devices:**
- iPhone 12/13/14 (iOS Safari)
- Samsung Galaxy S21/S22 (Chrome)
- iPad (Safari)

**Desktop Browsers:**
- Chrome (latest)
- Firefox (latest)
- Safari (latest)
- Edge (latest)

### Network Conditions

Test on various network speeds:
- Fast 3G (750ms latency)
- Slow 3G (2000ms latency)
- Offline (verify error handling)

---

## 🐛 Common Issues & Solutions

### Issue: Routes don't work after refresh
**Solution**: Configure hosting for SPA routing (see DEPLOYMENT.md)

### Issue: Environment variables not loading
**Solution**: Ensure variables start with `VITE_` and rebuild

### Issue: Animations stuttering
**Solution**: Check GPU acceleration, reduce animation complexity

### Issue: Mobile menu not closing
**Solution**: Verify backdrop click handler and state management

### Issue: Forms not submitting
**Solution**: Check form validation, console errors, network tab

---

## 📊 Performance Benchmarks

### Target Metrics
- **First Contentful Paint**: < 1.5s
- **Largest Contentful Paint**: < 2.5s
- **Time to Interactive**: < 3.5s
- **Cumulative Layout Shift**: < 0.1
- **First Input Delay**: < 100ms

### Tools
- Chrome DevTools Lighthouse
- WebPageTest
- Chrome DevTools Performance tab
- React DevTools Profiler

---

## ✅ Sign-off Checklist

Before marking as complete:

- [ ] All functionality tests pass
- [ ] Responsive design verified on 3+ devices
- [ ] Accessibility checks complete
- [ ] Browser compatibility verified
- [ ] Performance benchmarks met
- [ ] Error handling tested
- [ ] Documentation reviewed
- [ ] Deployment guide followed
- [ ] Production build tested
- [ ] Stakeholder approval received

---

## 📝 Test Report Template

```markdown
# Test Report - [Date]

## Environment
- Build: [version/commit]
- Browser: [name/version]
- Device: [device/OS]

## Tests Performed
- [ ] Functionality
- [ ] Responsive
- [ ] Accessibility
- [ ] Performance

## Issues Found
1. [Issue description]
   - Severity: [Critical/High/Medium/Low]
   - Steps to reproduce: [...]
   - Expected: [...]
   - Actual: [...]

## Overall Status
- [ ] Pass
- [ ] Pass with minor issues
- [ ] Fail

## Notes
[Additional observations]

## Tester
[Name]
```

---

**Last Updated**: January 2025
