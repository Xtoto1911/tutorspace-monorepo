import { BrowserRouter, Route, Routes, useLocation } from 'react-router-dom';
import { useEffect } from 'react';
import { PublicLayout, SectionLayout } from './components/layout';
import {
  ForgotPasswordPage,
  ParentRegistrationPage,
  ResetPasswordPage,
  SignInPage,
  SignUpChoicePage,
  TutorRegistrationPage,
} from './pages/public/AuthPages';
import { CatalogPage } from './pages/public/CatalogPage';
import { HomePage } from './pages/public/HomePage';
import { NotFoundPage } from './pages/public/NotFoundPage';
import {
  AboutPage as AboutStaticPage,
  ContactsPage as ContactsStaticPage,
  FaqPage as FaqStaticPage,
  HowItWorksPage as HowItWorksStaticPage,
  PrivacyPage,
  TermsPage,
} from './pages/public/StaticPages';
import { TutorProfilePage } from './pages/public/TutorProfilePage';
import { SectionPlaceholderPage } from './pages/sections/SectionPlaceholderPage';
import { roadmapSections } from './shared/config/roadmap';
import { AppEntryRedirect, RequireAuth } from './components/route-guards';
import { AdminAuditPage } from './pages/app/AdminAuditPage';
import { AdminDashboardPage } from './pages/app/AdminDashboardPage';
import { AdminSettingsPage } from './pages/app/AdminSettingsPage';
import { AdminSiteStatsPage } from './pages/app/AdminSiteStatsPage';
import { AdminSubscriptionsPage } from './pages/app/AdminSubscriptionsPage';
import { AdminUsersPage } from './pages/app/AdminUsersPage';
import { ClientApplicationDetailsPage } from './pages/app/ClientApplicationDetailsPage';
import { ClientApplicationsPage } from './pages/app/ClientApplicationsPage';
import { ClientFavoritesPage } from './pages/app/ClientFavoritesPage';
import { ClientHistoryPage } from './pages/app/ClientHistoryPage';
import { ClientMessagesPage } from './pages/app/ClientMessagesPage';
import { ClientSchedulePage } from './pages/app/ClientSchedulePage';
import { ClientSettingsPage } from './pages/app/ClientSettingsPage';
import { ModeratorComplaintsPage } from './pages/app/ModeratorComplaintsPage';
import { ModeratorOverviewPage } from './pages/app/ModeratorOverviewPage';
import { ModeratorReviewModerationPage } from './pages/app/ModeratorReviewModerationPage';
import { TutorCalendarPage } from './pages/app/TutorCalendarPage';
import { TutorMessagesPage } from './pages/app/TutorMessagesPage';
import { TutorRequestsPage } from './pages/app/TutorRequestsPage';
import { TutorSettingsPage } from './pages/app/TutorSettingsPage';
import { TutorStudentsPage } from './pages/app/TutorStudentsPage';
import { TutorSubscriptionPage } from './pages/app/TutorSubscriptionPage';
import { TutorTariffCheckoutPage } from './pages/app/TutorTariffCheckoutPage';
import { TutorWorkspaceProfilePage } from './pages/app/TutorWorkspaceProfilePage';
import { ClientOverviewPage } from './pages/app/ClientOverviewPage';
import { TutorOverviewPage } from './pages/app/TutorOverviewPage';

function renderSectionRoutes(sectionKey: 'client' | 'tutor' | 'admin' | 'moderator') {
  const section = roadmapSections.find((item) => item.key === sectionKey);

  if (!section) {
    return null;
  }

  return section.routes.map((route) => {
    const element =
      sectionKey === 'client' && route.index ? (
        <ClientOverviewPage />
      ) : sectionKey === 'client' && route.path === 'applications' ? (
        <ClientApplicationsPage />
      ) : sectionKey === 'client' && route.path === 'schedule' ? (
        <ClientSchedulePage />
      ) : sectionKey === 'client' && route.path === 'messages' ? (
        <ClientMessagesPage />
      ) : sectionKey === 'client' && route.path === 'favorites' ? (
        <ClientFavoritesPage />
      ) : sectionKey === 'client' && route.path === 'history' ? (
        <ClientHistoryPage />
      ) : sectionKey === 'client' && route.path === 'settings' ? (
        <ClientSettingsPage />
      ) : sectionKey === 'tutor' && route.index ? (
        <TutorOverviewPage />
      ) : sectionKey === 'tutor' && route.path === 'requests' ? (
        <TutorRequestsPage />
      ) : sectionKey === 'tutor' && route.path === 'calendar' ? (
        <TutorCalendarPage />
      ) : sectionKey === 'tutor' && route.path === 'students' ? (
        <TutorStudentsPage />
      ) : sectionKey === 'tutor' && route.path === 'messages' ? (
        <TutorMessagesPage />
      ) : sectionKey === 'tutor' && route.path === 'subscription' ? (
        <TutorSubscriptionPage />
      ) : sectionKey === 'tutor' && route.path === 'settings' ? (
        <TutorSettingsPage />
      ) : sectionKey === 'tutor' && route.path === 'profile' ? (
        <TutorWorkspaceProfilePage />
      ) : sectionKey === 'admin' && route.index ? (
        <AdminDashboardPage />
      ) : sectionKey === 'admin' && route.path === 'users' ? (
        <AdminUsersPage />
      ) : sectionKey === 'admin' && route.path === 'site-stats' ? (
        <AdminSiteStatsPage />
      ) : sectionKey === 'admin' && route.path === 'subscriptions' ? (
        <AdminSubscriptionsPage />
      ) : sectionKey === 'admin' && route.path === 'audit' ? (
        <AdminAuditPage />
      ) : sectionKey === 'admin' && route.path === 'settings' ? (
        <AdminSettingsPage />
      ) : sectionKey === 'moderator' && route.index ? (
        <ModeratorOverviewPage />
      ) : sectionKey === 'moderator' && route.path === 'complaints' ? (
        <ModeratorComplaintsPage />
      ) : sectionKey === 'moderator' && route.path === 'review-moderation' ? (
        <ModeratorReviewModerationPage />
      ) : (
        <SectionPlaceholderPage sectionKey={sectionKey} route={route} />
      );
    return route.index ? (
      <Route key={`${sectionKey}-index`} index element={element} />
    ) : (
      <Route key={`${sectionKey}-${route.path}`} path={route.path} element={element} />
    );
  });
}

function ScrollToTop() {
  const { pathname, hash } = useLocation();

  useEffect(() => {
    if (!hash) {
      window.scrollTo(0, 0);
    }
  }, [pathname, hash]);

  return null;
}

export function App() {
  return (
    <BrowserRouter>
      <ScrollToTop />
      <Routes>
        <Route element={<PublicLayout />}>
          <Route index element={<HomePage />} />
          <Route path="catalog" element={<CatalogPage />} />
          <Route path="catalog/subject/:subjectSlug" element={<CatalogPage />} />
          <Route path="tutors/:slug" element={<TutorProfilePage />} />
          <Route path="how-it-works" element={<HowItWorksStaticPage />} />
          <Route path="about" element={<AboutStaticPage />} />
          <Route path="faq" element={<FaqStaticPage />} />
          <Route path="contacts" element={<ContactsStaticPage />} />
          <Route path="privacy" element={<PrivacyPage />} />
          <Route path="terms" element={<TermsPage />} />
        </Route>

        <Route path="auth" element={<PublicLayout />}>
          <Route index element={<SignInPage />} />
          <Route path="sign-in" element={<SignInPage />} />
          <Route path="sign-up" element={<SignUpChoicePage />} />
          <Route path="sign-up/parent" element={<ParentRegistrationPage />} />
          <Route path="sign-up/tutor" element={<TutorRegistrationPage />} />
          <Route path="forgot-password" element={<ForgotPasswordPage />} />
          <Route path="reset-password" element={<ResetPasswordPage />} />
          <Route path="reset-password/:token" element={<ResetPasswordPage />} />
        </Route>

        <Route path="app" element={<AppEntryRedirect />} />

        <Route
          path="app/client"
          element={
            <RequireAuth allowedRoles={['parent']}>
              <SectionLayout sectionKey="client" />
            </RequireAuth>
          }
        >
          <Route path="applications/:applicationId" element={<ClientApplicationDetailsPage />} />
          {renderSectionRoutes('client')}
        </Route>

        <Route
          path="app/tutor"
          element={
            <RequireAuth allowedRoles={['tutor']}>
              <SectionLayout sectionKey="tutor" />
            </RequireAuth>
          }
        >
          <Route path="subscription/checkout" element={<TutorTariffCheckoutPage />} />
          <Route path="subscription/checkout/result" element={<TutorTariffCheckoutPage />} />
          {renderSectionRoutes('tutor')}
        </Route>

        <Route
          path="admin"
          element={
            <RequireAuth allowedRoles={['admin']}>
              <SectionLayout sectionKey="admin" />
            </RequireAuth>
          }
        >
          {renderSectionRoutes('admin')}
        </Route>

        <Route
          path="moderator"
          element={
            <RequireAuth allowedRoles={['admin', 'moderator']}>
              <SectionLayout sectionKey="moderator" />
            </RequireAuth>
          }
        >
          {renderSectionRoutes('moderator')}
        </Route>

        <Route path="*" element={<NotFoundPage />} />
      </Routes>
    </BrowserRouter>
  );
}
