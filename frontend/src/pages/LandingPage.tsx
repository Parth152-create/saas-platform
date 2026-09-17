import React, { useEffect } from 'react';
import { LandingNavbar } from '../components/landing/LandingNavbar';
import { HeroSection } from '../components/landing/HeroSection';
import { ProductPreview } from '../components/landing/ProductPreview';
import { PlatformOverview } from '../components/landing/PlatformOverview';
import { ProblemSolutionSection } from '../components/landing/ProblemSolutionSection';
import { WorkforceSection } from '../components/landing/WorkforceSection';
import { OperationsSection } from '../components/landing/OperationsSection';
import { AnalyticsSection } from '../components/landing/AnalyticsSection';
import { SecuritySection } from '../components/landing/SecuritySection';
import { PricingSection } from '../components/landing/PricingSection';
import { HowItWorks } from '../components/landing/HowItWorks';
import { FinalCTA } from '../components/landing/FinalCTA';
import { LandingFooter } from '../components/landing/LandingFooter';

export const LandingPage: React.FC = () => {
  useEffect(() => {
    // Document SEO
    const originalTitle = document.title;
    document.title = 'SaaS Platform — Enterprise Workforce & Business Operations';

    const metaDescription = document.querySelector('meta[name="description"]');
    const originalDescription = metaDescription?.getAttribute('content') || '';
    if (metaDescription) {
      metaDescription.setAttribute(
        'content',
        'Manage your workforce, projects, time, schedules and business operations from one secure multi-tenant SaaS platform.'
      );
    }

    return () => {
      document.title = originalTitle;
      if (metaDescription) {
        metaDescription.setAttribute('content', originalDescription);
      }
    };
  }, []);

  return (
    <div className="min-h-screen bg-white dark:bg-[#0a0a0a] text-neutral-900 dark:text-neutral-100 font-sans selection:bg-neutral-900 selection:text-white dark:selection:bg-white dark:selection:text-neutral-900">
      {/* Navigation Bar */}
      <LandingNavbar />

      {/* Main Content Sections */}
      <main id="main-content">
        {/* 1. Hero Section */}
        <HeroSection />

        {/* 2. Live Mockup Product Preview */}
        <ProductPreview />

        {/* 3. Platform Capabilities Overview */}
        <PlatformOverview />

        {/* 4. Problem to Solution Consolidation */}
        <ProblemSolutionSection />

        {/* 5. Workforce & Human Resource Management */}
        <WorkforceSection />

        {/* 6. Projects & Operations Lifecycle */}
        <OperationsSection />

        {/* 7. Reports & Analytics */}
        <AnalyticsSection />

        {/* 8. Security & Multi-Tenancy Architecture */}
        <SecuritySection />

        {/* 9. Subscription Tiers & Feature Matrix */}
        <PricingSection />

        {/* 10. How It Works (Onboarding Journey) */}
        <HowItWorks />

        {/* 11. Final High-Impact Conversion CTA */}
        <FinalCTA />
      </main>

      {/* Footer */}
      <LandingFooter />
    </div>
  );
};

export default LandingPage;
