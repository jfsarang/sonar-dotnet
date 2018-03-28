﻿/*
 * SonarAnalyzer for .NET
 * Copyright (C) 2015-2018 SonarSource SA
 * mailto: contact AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

using System.Collections.Generic;
using System.Linq;
using System.Resources;
using Microsoft.CodeAnalysis;

namespace SonarAnalyzer.Helpers
{
    public static class DiagnosticDescriptorBuilder
    {
        internal const string SonarWayTag = "SonarWay";

        public static DiagnosticDescriptor GetUtilityDescriptor(string diagnosticId, string title) =>
            new DiagnosticDescriptor(
                diagnosticId,
                title,
                string.Empty,
                string.Empty,
                DiagnosticSeverity.Warning,
                isEnabledByDefault: true,
                customTags: BuildUtilityCustomTags().ToArray());

        public static DiagnosticDescriptor GetDescriptor(string diagnosticId, string messageFormat,
            ResourceManager resourceManager, bool? isEnabledByDefault = null) =>
            new DiagnosticDescriptor(
                diagnosticId,
                resourceManager.GetString($"{diagnosticId}_Title"),
                messageFormat,
                resourceManager.GetString($"{diagnosticId}_Category"),
                DiagnosticSeverity.Warning, // We want all rules to be warning by default
                isEnabledByDefault ?? bool.Parse(resourceManager.GetString($"{diagnosticId}_IsActivatedByDefault")),
                helpLinkUri: GetHelpLink(resourceManager, diagnosticId),
                description: resourceManager.GetString($"{diagnosticId}_Description"),
                customTags: BuildCustomTags(diagnosticId, resourceManager).ToArray());

        public static string GetHelpLink(ResourceManager resourceManager, string diagnosticId) =>
            string.Format(resourceManager.GetString("HelpLinkFormat"), diagnosticId.Substring(1));

        private static IEnumerable<string> BuildCustomTags(string diagnosticId, ResourceManager resourceManager)
        {
            if (bool.Parse(resourceManager.GetString($"{diagnosticId}_IsActivatedByDefault")))
            {
                yield return SonarWayTag;
            }

            yield return resourceManager.GetString("RoslynLanguage");
        }

        private static IEnumerable<string> BuildUtilityCustomTags()
        {
            // Allow to configure the analyzers in debug mode only.
            // This allows to run test selectively (for example to test only one rule)
#if DEBUG
            yield break;
#else
            yield return Microsoft.CodeAnalysis.WellKnownDiagnosticTags.NotConfigurable;
#endif

        }
    }
}
